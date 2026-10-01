package com.hoid.voidlauncher

import android.app.Application
import com.hoid.voidlauncher.di.AppContainer

/**
 * Owns the object graph for the process.
 *
 * DI is manual (see docs/architecture.md §3.1). [AppContainer] constructs
 * everything lazily, so nothing is built until something actually asks for it —
 * which matters more than usual for a launcher, where cold start to an
 * interactive home screen is a stated target of under 500 ms.
 */
class VoidApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
