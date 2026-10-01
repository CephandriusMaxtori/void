package com.hoid.voidlauncher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoid.voidlauncher.core.data.AppEntry
import com.hoid.voidlauncher.di.AppContainer
import com.hoid.voidlauncher.feature.drawer.DrawerContent
import com.hoid.voidlauncher.feature.home.HomeGridSpec
import com.hoid.voidlauncher.feature.home.HomePageContent
import kotlinx.coroutines.flow.collectLatest

/**
 * The root of the launcher: a pager of home pages, with the drawer sliding over.
 *
 * Stateless with respect to data. Everything comes from [LauncherUiState] and
 * every interaction goes back through the ViewModel, so the only things this
 * function owns are gesture handling and animation.
 */
@Composable
fun LauncherRoot(
    container: AppContainer,
    modifier: Modifier = Modifier,
    viewModel: LauncherViewModel = viewModel(factory = LauncherViewModel.factory(container)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = container.haptics

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (state.pageCount > 0) {
            HomePager(
                state = state,
                onPageSelected = viewModel::onPageSelected,
                onOpenApp = viewModel::onAppClicked,
            )
        }

        DrawerOverlay(
            visible = state.drawerOpen,
            apps = state.apps,
            onOpenApp = viewModel::onAppClicked,
        )

        // Only shown with more than one page. A single dot on a one-page
        // launcher is noise, and the design is built on not adding noise.
        if (state.pageCount > 1) {
            PageIndicator(
                count = state.pageCount,
                selected = state.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
            )
        }
    }

    // Predictive back: while the drawer is open, back closes it. The system
    // routes the gesture to the HOME activity and expects it consumed here —
    // letting it through would predict a "leave home" transition that never
    // completes, because a home app has nothing behind it to reveal.
    BackHandler(enabled = state.drawerOpen) {
        haptics.confirm()
        viewModel.onDrawerClosed()
    }
}

@Composable
private fun HomePager(
    state: LauncherUiState,
    onPageSelected: (Int) -> Unit,
    onOpenApp: (String) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.pageCount },
    )

    // Report position upward by observing the pager rather than passing an
    // onPageChanged callback, which keeps this composable stateless and lets the
    // pager implementation change without touching the call site.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest(onPageSelected)
    }

    HorizontalPager(
        state = pagerState,
        // Design doc §4.2: side insets so One UI's edge-back gesture does not
        // collide with a page swipe. A page the user cannot swipe away from the
        // left edge is worse than the extra navigation.
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 8.dp,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        HomePageContent(
            page = state.pages[page],
            dockApps = state.dockApps,
            onOpenApp = onOpenApp,
        )
    }
}

@Composable
private fun DrawerOverlay(
    visible: Boolean,
    apps: List<AppEntry>,
    onOpenApp: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }

    // Reset the query on close, so reopening the drawer does not present a
    // stale filter the user has forgotten about.
    LaunchedEffect(visible) {
        if (!visible) query = ""
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        DrawerContent(
            apps = apps,
            query = query,
            onQueryChange = { query = it },
            onOpenApp = onOpenApp,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
    }
}

/**
 * Page dots.
 *
 * Hand-drawn rather than a Material component because the design language is a
 * monochrome dot (design doc §4.4): the indicator should read as the same motif
 * as the clock and the app icon.
 */
@Composable
private fun PageIndicator(
    count: Int,
    selected: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count.coerceAtMost(HomeGridSpec.MAX_PAGES)) { index ->
            val color = if (index == selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
