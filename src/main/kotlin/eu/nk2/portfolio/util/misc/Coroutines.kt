package eu.nk2.portfolio.util.misc

import kotlinx.coroutines.CompletableDeferred

val <T> T.deferred: CompletableDeferred<T>
    get() = CompletableDeferred(this)
