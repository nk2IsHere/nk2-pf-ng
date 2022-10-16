package eu.nk2.portfolio.util.control

import kotlinx.coroutines.*

suspend inline fun <T> async(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: suspend () -> T): Deferred<T> =
    withContext(dispatcher) {
        async {
            supplier()
        }
    }

suspend inline fun <T: Any> optionAsync(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: suspend () -> T?): Deferred<Option<T>> =
    withContext(dispatcher) {
        async {
            supplier()
                ?.option
                ?: nothing()
        }
    }

suspend inline fun <T: Any> tryAsync(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: suspend () -> T): Deferred<Try<T>> =
    withContext(dispatcher) {
        async {
            Try {
                supplier()
            }
        }
    }

suspend inline fun <T: Any> tryOptionAsync(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: suspend () -> T?): Deferred<Try<Option<T>>> =
    withContext(dispatcher) {
        async {
            Try {
                supplier()
                    ?.option
                    ?: nothing()
            }
        }
    }