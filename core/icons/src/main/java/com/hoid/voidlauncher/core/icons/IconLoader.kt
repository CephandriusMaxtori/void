package com.hoid.voidlauncher.core.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.util.LruCache
import com.hoid.voidlauncher.core.system.LauncherAppsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Loads app icons with the v1 monochrome pipeline.
 *
 * Design doc §4.3: prefer an app's own monochrome layer when the platform has it,
 * otherwise fall back to a grayscale transform. The output is then cached in both
 * memory and on disk so a launcher never pays the decode cost twice on the same
 * app during the same session.
 */
class IconLoader(
    context: Context,
    private val launcherApps: LauncherAppsSource,
    private val themeVersion: Int = 1,
) {
    private val appContext: Context = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    private val memoryCache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8L).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val diskCacheDir = File(appContext.cacheDir, "app-icons").apply { mkdirs() }

    suspend fun loadBitmap(componentKey: String): Bitmap = withContext(Dispatchers.Default) {
        val cacheKey = cacheKeyFor(componentKey)

        memoryCache.get(cacheKey)?.let { return@withContext it }

        readDisk(cacheKey)?.let { cached ->
            memoryCache.put(cacheKey, cached)
            return@withContext cached
        }

        val rendered = renderBitmap(componentKey)
        memoryCache.put(cacheKey, rendered)
        writeDisk(cacheKey, rendered)
        rendered
    }

    fun cacheKeyFor(componentKey: String): String {
        val versionCode = launcherApps.versionCode(componentKey) ?: 0L
        return "$componentKey|$versionCode|$themeVersion"
    }

    private fun renderBitmap(componentKey: String): Bitmap {
        val drawable = resolveDrawable(componentKey) ?: return fallbackBitmap()
        val monochrome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (drawable as? AdaptiveIconDrawable)?.monochrome
        } else {
            null
        }

        val bitmap = drawableToBitmap(monochrome ?: drawable)
        return if (monochrome != null) {
            bitmap
        } else {
            grayscaleBitmap(bitmap)
        }
    }

    private fun resolveDrawable(componentKey: String): Drawable? {
        val packageName = componentKey.substringBefore('/', "")
        val className = componentKey.substringAfter('/', "").substringBefore('#', "")

        if (packageName.isBlank() || className.isBlank()) {
            return null
        }

        val activity = ComponentName(packageName, className)

        return try {
            packageManager.getActivityIcon(activity)
        } catch (_: PackageManager.NameNotFoundException) {
            try {
                packageManager.getApplicationIcon(packageName)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(Rect(0, 0, size, size))
        drawable.draw(canvas)
        return bitmap
    }

    private fun grayscaleBitmap(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix().apply {
                    setSaturation(0f)
                },
            )
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    private fun fallbackBitmap(): Bitmap {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint().apply { color = Color.DKGRAY }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), background)

        val glyph = Paint().apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            textSize = 46f
        }
        val text = "?"
        val bounds = Rect()
        glyph.getTextBounds(text, 0, text.length, bounds)
        canvas.drawText(text, size / 2f, (size / 2f) - (bounds.exactCenterY()), glyph)
        return bitmap
    }

    private fun readDisk(cacheKey: String): Bitmap? {
        val file = diskFile(cacheKey)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Disk icon cache read failed for $cacheKey", e)
            null
        }
    }

    private fun writeDisk(cacheKey: String, bitmap: Bitmap) {
        val file = diskFile(cacheKey)
        try {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Disk icon cache write failed for $cacheKey", e)
        }
    }

    private fun diskFile(cacheKey: String): File =
        File(diskCacheDir, "icon_${cacheKey.hashCode()}.png")

    companion object {
        private const val TAG = "IconLoader"
    }
}
