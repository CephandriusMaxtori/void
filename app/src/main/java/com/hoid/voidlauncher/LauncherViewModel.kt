package com.hoid.voidlauncher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hoid.voidlauncher.core.data.AppEntry
import com.hoid.voidlauncher.core.data.AppRepository
import com.hoid.voidlauncher.core.data.HomePage
import com.hoid.voidlauncher.core.system.LauncherAppsSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the launcher screen shows, in one immutable snapshot.
 *
 * A single state object rather than several independent flows. The home screen
 * and the drawer are two views of one screen, and separate flows for "pages",
 * "apps", "is the drawer open", and "which page" would need reconciling at
 * every render — which is how a launcher ends up showing the drawer open over
 * an empty home screen.
 */
data class LauncherUiState(
    val pages: List<HomePage> = emptyList(),
    val apps: List<AppEntry> = emptyList(),
    val currentPage: Int = 0,
    val drawerOpen: Boolean = false,
    val dockApps: List<String> = emptyList(),
    val loading: Boolean = true,
) {
    val pageCount: Int get() = pages.size
}

/**
 * Coordinates the home screen and the drawer.
 *
 * Lives in `app` rather than in either feature module because it is the thing
 * that coordinates *them*. `feature:home` and `feature:drawer` stay stateless and
 * know nothing about each other, which is what lets either be replaced without
 * touching the other.
 */
class LauncherViewModel(
    private val repository: AppRepository,
    private val launcherApps: LauncherAppsSource,
) : ViewModel() {

    private val currentPage = MutableStateFlow(0)
    private val drawerOpen = MutableStateFlow(false)

    val uiState: StateFlow<LauncherUiState> = combine(
        repository.observePages(),
        repository.observeVisibleApps(),
        currentPage,
        drawerOpen,
    ) { pages, apps, page, drawer ->
        LauncherUiState(
            pages = pages,
            apps = apps,
            // Clamped rather than trusted: pages can be deleted from the
            // database while the pager still holds the old index, and an
            // out-of-bounds page index crashes the pager rather than degrading.
            currentPage = page.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
            drawerOpen = drawer,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        // Keeps the flow alive briefly across a configuration change so a
        // rotation does not re-enumerate every installed app.
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState(),
    )

    init {
        // Guarantees at least one page exists. Doing it here means the pager
        // never has to render a zero-page state, which has no sensible empty
        // form.
        viewModelScope.launch {
            repository.ensurePages(minimum = 1)
        }
    }

    fun onPageSelected(index: Int) {
        currentPage.value = index
    }

    fun onDrawerOpened() {
        drawerOpen.value = true
    }

    fun onDrawerClosed() {
        drawerOpen.value = false
    }

    fun onDrawerToggled() {
        drawerOpen.value = !drawerOpen.value
    }

    /**
     * Launches an app and closes the drawer.
     *
     * The lookup goes through the live app list rather than resolving the
     * component directly, so an app that vanished since the list was drawn fails
     * quietly instead of throwing `ActivityNotFoundException` at the user.
     */
    fun onAppClicked(componentKey: String) {
        val app = uiState.value.apps.firstOrNull { it.componentKey == componentKey }
        if (app == null) return
        drawerOpen.value = false
        // Launching needs the SystemApp, which only core:system can build.
        // Resolution is deferred to a scope that is not the UI thread.
        viewModelScope.launch {
            launcherApps.launchByKey(componentKey)
        }
    }

    fun onAddPage() {
        viewModelScope.launch {
            repository.addPage()
        }
    }

    fun onRemovePage(index: Int) {
        val page = uiState.value.pages.getOrNull(index) ?: return
        viewModelScope.launch {
            repository.removePage(page.id)
        }
    }

    fun onMovePage(delta: Int) {
        val state = uiState.value
        val from = state.currentPage
        val to = from + delta
        if (to !in state.pages.indices) return

        val orderedIds = state.pages.map { it.id }.toMutableList()
        val movedPage = orderedIds.removeAt(from)
        orderedIds.add(to, movedPage)
        currentPage.value = to
        viewModelScope.launch {
            repository.reorderPages(orderedIds)
        }
    }

    companion object {
        /**
         * Factory, built by hand to match the manual-DI rule: `app` is the only
         * module that knows how to construct this, and everything else receives
         * its collaborators as constructor parameters.
         */
        fun factory(container: com.hoid.voidlauncher.di.AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    LauncherViewModel(
                        repository = container.appRepository,
                        launcherApps = container.launcherApps,
                    )
                }
            }
    }
}
