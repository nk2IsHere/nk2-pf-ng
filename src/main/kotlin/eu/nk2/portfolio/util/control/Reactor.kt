package eu.nk2.portfolio.util.control

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

suspend inline fun <T> monoTryOptionAsync(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: () -> Mono<T>): Deferred<Try<Option<T>>> =
    withContext(dispatcher) {
        async {
            Try {
                supplier()
                    .map { it.option }
                    .defaultIfEmpty(nothing())
                    .awaitSingle()
            }
        }
    }

suspend inline fun <T> monoTryOption(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: () -> Mono<T>): Try<Option<T>> =
    withContext(dispatcher) {
        Try {
            supplier()
                .map { it.option }
                .defaultIfEmpty(nothing())
                .awaitSingle()
        }
    }


suspend inline fun <T: Any> monoTryAsync(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: () -> Mono<T>): Deferred<Try<T>> =
    withContext(dispatcher) {
        async {
            Try {
                supplier()
                    .awaitSingle()
            }
        }
    }

suspend inline fun <T: Any> monoTry(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: () -> Mono<T>): Try<T> =
    withContext(dispatcher) {
        Try {
            supplier()
                .awaitSingle()
        }
    }


suspend inline fun <T: Any> monoOptionAsync(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: () -> Mono<T>): Deferred<Option<T>> =
    withContext(dispatcher) {
        async {
            supplier()
                .map { it.option }
                .defaultIfEmpty(nothing())
                .awaitSingle()
        }
    }

suspend inline fun <T: Any> monoOption(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: () -> Mono<T>): Option<T> =
    withContext(dispatcher) {
        supplier()
            .map { it.option }
            .defaultIfEmpty(nothing())
            .awaitSingle()
    }

suspend inline fun <T: Any> monoAsync(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: () -> Mono<T>): Deferred<T> =
    withContext(dispatcher) {
        async {
            supplier()
                .awaitSingle()
        }
    }

suspend inline fun <T: Any> fluxTryAsync(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: () -> Flux<T>): Deferred<Try<Flow<T>>> =
    withContext(dispatcher) {
        async {
            Try {
                supplier()
                    .asFlow()
            }
        }
    }

suspend inline fun <T: Any> fluxTry(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: () -> Flux<T>): Try<Flow<T>> =
    withContext(dispatcher) {
        Try {
            supplier()
                .asFlow()
        }
    }


suspend inline fun <T: Any> fluxAsync(dispatcher: CoroutineDispatcher = Dispatchers.IO, crossinline supplier: () -> Flux<T>): Deferred<Flow<T>> =
    withContext(dispatcher) {
        async {
            supplier()
                .asFlow()
        }
    }
