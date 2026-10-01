package com.hoid.voidlauncher.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hoid.voidlauncher.core.data.GridItem
import com.hoid.voidlauncher.core.data.HomePage

/**
 * Grid geometry (design doc §4.2: fixed N×M cells, default 4×6).
 *
 * Configurable from settings in M7. Fixed now so the layout math has one place
 * to live and the grid and the dock can agree on a cell size.
 */
object HomeGridSpec {
    const val COLUMNS = 4
    const val ROWS = 6
    const val DOCK_COLUMNS = 4

    /** Hard ceiling. A pager with 40 pages is not navigable. */
    const val MAX_PAGES = 7
}

/**
 * A single home page: a fixed grid plus the dock row.
 *
 * Stateless. It renders what it is given and reports intent upward, which is
 * what lets a page recompose on a scroll without knowing the repository exists.
 */
@Composable
fun HomePageContent(
    page: HomePage,
    dockApps: List<String>,
    onOpenApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (page.items.isEmpty()) {
                Text(
                    text = "Empty page",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                LazyVerticalGrid(
                    // Fixed column count, not adaptive. The grid is defined by
                    // HomeGridSpec, and an adaptive grid would silently change
                    // the column count with the window width, which would move
                    // icons on rotation.
                    columns = GridCells.Fixed(HomeGridSpec.COLUMNS),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = page.items,
                        // Stable keys from the database id, not the position or
                        // the component key. Drag-and-drop moves items between
                        // indices constantly, and an unstable key makes Compose
                        // re-create every cell that moved.
                        key = { it.id },
                    ) { item ->
                        GridCell(
                            item = item,
                            onOpen = { key -> onOpenApp(key) },
                        )
                    }
                }
            }
        }

        DockRow(
            componentKeys = dockApps,
            onOpenApp = onOpenApp,
        )
    }
}

@Composable
private fun GridCell(
    item: GridItem,
    onOpen: (String) -> Unit,
) {
    val label = when (item) {
        is GridItem.App -> item.componentKey.shortLabel()
        is GridItem.Folder -> "Folder"
        is GridItem.Widget -> "Widget"
    }
    val openable = item as? GridItem.App

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (openable != null) {
                    Modifier.clickable { onOpen(openable.componentKey) }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Placeholder for the monochrome icon, which arrives in M2. Sized like
        // an icon so the cell geometry does not move when it lands.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Transparent),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(2.dp),
        )
    }
}

@Composable
private fun DockRow(
    componentKeys: List<String>,
    onOpenApp: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Fixed count so the dock occupies the same width whether it holds one
        // app or four, which keeps icons from sliding sideways as the user adds
        // them.
        repeat(HomeGridSpec.DOCK_COLUMNS) { slot ->
            val key = componentKeys.getOrNull(slot)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (key != null) {
                            Modifier.clickable { onOpenApp(key) }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (key != null) {
                    Text(
                        text = key.shortLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(2.dp),
                    )
                }
            }
        }
    }
}

/**
 * A readable stand-in for `package/class#serial`.
 *
 * Real labels arrive from `LauncherApps` in M2 along with the icons; deriving
 * them from the key keeps the grid honest about what it has right now.
 */
private fun String.shortLabel(): String =
    substringAfter('/').substringAfterLast('.').substringBefore('#')
