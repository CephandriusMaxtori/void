package com.hoid.voidlauncher.feature.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoid.voidlauncher.core.data.AppEntry

/**
 * The app drawer: a search field over a flat, alphabetical list.
 *
 * Categories arrive in M4. The list is already sorted by label in the
 * repository, so this renders it rather than sorting again — two sort orders
 * would eventually disagree.
 */
@Composable
fun DrawerContent(
    apps: List<AppEntry>,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Filtered here rather than in the ViewModel so typing stays local and
    // instant. `remember` keyed on both, because the list changes underneath
    // as packages are installed and removed.
    val filtered = remember(apps, query) { filterApps(apps, query) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            label = { Text("Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (query.isBlank()) "No apps" else "No matches",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                items = filtered,
                // componentKey is unique per app per profile, so this is a
                // correct, stable key. An index would not be: the list re-sorts
                // as the query changes.
                key = { it.componentKey },
            ) { app ->
                AppRow(app = app, onOpen = onOpenApp)
            }
        }
    }
}

/**
 * Substring match on label, with a package-prefix fallback.
 *
 * Deliberately not fuzzy and not pinyin/subsequence matching. A launcher
 * search that returns eight near-misses is worse than one that returns the one
 * app meant, and the near-miss problem gets worse as the app count grows.
 */
internal fun filterApps(apps: List<AppEntry>, query: String): List<AppEntry> {
    val q = query.trim()
    if (q.isEmpty()) return apps

    return apps.filter { app ->
        app.label.contains(q, ignoreCase = true) ||
            app.componentKey.substringBefore('/').contains(q, ignoreCase = true)
    }
}

@Composable
private fun AppRow(
    app: AppEntry,
    onOpen: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpen(app.componentKey) }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sized like the monochrome icon, which lands in M2. Fixing the size
        // now means the row does not reflow when the real icon arrives.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        // The category chip becomes an interactive tab in M4. Rendering it now
        // means the M4 change is a behaviour change, not a layout change.
        Text(
            text = app.category.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
