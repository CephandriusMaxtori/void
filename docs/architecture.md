# Architecture

Companion to [`design-v0.3.md`](design-v0.3.md). That doc says *what* the
launcher is. This one says *how the code is arranged* and what rules keep it
that way.

## Shape

Single activity, MVVM, unidirectional data flow.

```
User gesture
    -> Compose UI (feature:*)
    -> ViewModel  (state holder, no Android framework deps beyond coroutines)
    -> Repository (core:data / core:system)
    -> StateFlow  (single source of truth)
    -> back down to Compose, which renders it
```

UI never mutates state directly. It calls the ViewModel, the ViewModel updates a
`StateFlow`, and Compose re-renders from the flow. There are no two-way bindings
and no imperative "tell the grid to scroll" calls.

## Module graph

```
                         app
                          |
        +--------+--------+--------+--------+
        |        |        |        |        |
    feature  feature  feature  feature  (di wiring only)
    :home    :drawer  :widgets  :settings
        |        |        |        |
        +--------+--------+--------+
                     |
              core:designsystem
              core:icons
                     |
              core:data
              core:system
```

### Dependency rules

These are the rules that make the boundaries real. Violating them is a bug, not
a style preference.

1. **Dependencies point downward only.** `feature:*` may use `core:*`. `core:*`
   never imports from `feature:*` or `app`. No cycles.
2. **`core:designsystem` depends on nothing internal.** Tokens, typography,
   haptics. It is the shared vocabulary; if it needed anything else, that
   something belongs in a different module.
3. **`core:icons` must not depend on `core:data`.** It is handed an icon key and
   returns a drawable. It has no idea what an app *is*.
4. **`core:system` is the only module that talks to `LauncherApps`,**
   `AppWidgetManager`, or `PackageManager`. Everything else consumes its
   abstractions. This is what makes `core:system` swappable and what keeps
   widget-hosting crashes contained.
5. **`core:data` owns all persistence.** Room and DataStore. No other module
   opens a database.
6. **`app` is the only module that knows about DI.** Everything else receives
   its dependencies through constructor parameters.

Enforcement is by convention first. If it starts being violated in practice, the
fix is either to move code to the right module or to add a lint rule — not to
relax the rule.

## Dependency injection

Manual, via a single `AppContainer` owned by `app`.

```kotlin
class AppContainer(context: Context) {
    val appRepository: AppRepository by lazy { ... }
    val settings: SettingsStore by lazy { ... }
    val iconLoader: IconLoader by lazy { ... }
    val haptics: Haptics by lazy { ... }
}

class LauncherActivity : ComponentActivity() {
    private val container by lazy { (application as VoidApplication).container }
    // ViewModels get factories that close over `container`
}
```

`by lazy` is deliberate: it means nothing is constructed until something
actually asks for it, which keeps cold start cheap. That matters more than usual
for a launcher, where cold start to interactive home is a stated target of
under 500 ms.

ViewModels are constructed with `viewModelFactory { initializer { ... } }` so
they receive exactly the collaborators they need and nothing more. That is also
what makes them testable with plain fakes.

**No annotation processor is used for DI.** KSP is reserved for Room. This keeps
builds fast and removes a whole class of plugin-compatibility risk.

## State ownership

| State | Owner | Persistence |
|---|---|---|
| Installed apps, labels, categories | `core:data` `AppRepository` | Room (categories/overrides) |
| Home layout, pages, folders, dock | `core:data` `LayoutRepository` | Room |
| Widget placements | `core:data` | Room |
| User settings (grid size, theme, gestures) | `core:data` `SettingsStore` | DataStore |
| Transient UI (drag state, drawer scroll) | ViewModel `StateFlow` | none |

The line: anything the user would miss after a reboot is in Room or DataStore.
Anything that is just "what is on screen right now" lives in a ViewModel.

## Error handling

A launcher that crashes leaves the user with no home screen. That is the worst
failure mode in this app, so:

- **Widget hosting is isolated** in `core:system` behind a guard. A vendor
  widget that throws on measure is contained; the host degrades rather than
  propagating.
- **Package callbacks are treated as hostile input.** Apps are installed and
  uninstalled while the launcher runs. Every `LauncherApps.Callback` event
  re-derives state from scratch rather than patching it incrementally.
- **Icon rendering never blocks the main thread** and never throws into
  composition. A failed load renders the Material placeholder.
- **Coroutines in ViewModels are scoped to `viewModelScope`.** A rotation cannot
  leave orphaned work touching a destroyed hierarchy.

## Testing strategy

| Layer | Tool | Needs a device |
|---|---|---|
| Repositories, categorisation, layout math | JVM unit tests, fakes | no |
| ViewModels | JVM unit tests, `kotlinx-coroutines-test` | no |
| Design system, dot-matrix renderer | JVM unit tests, Compose UI test | no |
| Composables | Compose UI test (`createComposeRule`) | yes |
| Icon pipeline, widget host | instrumented | yes |
| Startup, scroll, drag | Macrobenchmark | yes |

The design goal is that the large majority of logic is testable on the JVM
without a device. That is a direct payoff of the manual-DI and interface-based
`core:system` choices: no Android framework in the constructor of anything
worth testing.
