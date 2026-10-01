package com.hoid.voidlauncher.di

import android.content.Context

/**
 * The object graph, assembled by hand.
 *
 * Every dependency is created here and nowhere else. Nothing below `app` knows
 * how to build its own collaborators; they receive them as constructor
 * parameters. That is what makes the ViewModels and repositories testable with
 * plain fakes and no test runner infrastructure.
 *
 * All properties are `by lazy` on purpose: constructing a repository eagerly
 * would mean opening a database and querying the package manager on the main
 * thread during `Application.onCreate`, which is the single easiest way to
 * blow a cold-start budget.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    // Populated as the milestones land. Deliberately empty right now: each entry
    // is added in the milestone that needs it, so the graph always reflects what
    // actually exists rather than what is planned.
}
