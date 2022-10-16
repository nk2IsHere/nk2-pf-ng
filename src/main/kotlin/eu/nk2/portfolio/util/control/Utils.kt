package eu.nk2.portfolio.util.control

inline fun <T: Any, R: Any> Try<Option<T>>.flatMapTryOptionBlocking(crossinline f: (T) -> Try<Option<R>>): Try<Option<R>> =
    this
        .fold(
            { it.rewrap() },
            { nothing<R>().wrap },
            { f(it) }
        )

suspend inline fun <T: Any, R: Any> Try<Option<T>>.flatMapTryOption(crossinline f: suspend (T) -> Try<Option<R>>): Try<Option<R>> =
    this
        .fold(
            { it.rewrap() },
            { nothing<R>().wrap },
            { f(it) }
        )

inline fun <T: Any, R: Any> Try<Option<T>>.flatMapTryBlocking(crossinline f: (T) -> Try<R>): Try<Option<R>> =
    this
        .popLeft()
        .flatMapRight(f)
        .popLeft()

suspend inline fun <T: Any, R: Any> Try<Option<T>>.flatMapTry(crossinline f: suspend (T) -> Try<R>): Try<Option<R>> =
    this
        .popLeft()
        .flatMapRight { f(it) }
        .popLeft()

inline fun <T: Any, R: Any> Try<Option<T>>.flatMapOptionBlocking(crossinline f: (T) -> Option<R>): Try<Option<R>> =
    this.flatMapRight(f)

suspend inline fun <T: Any, R: Any> Try<Option<T>>.flatMapOption(crossinline f: suspend (T) -> Option<R>): Try<Option<R>> =
    this.flatMapRight { f(it) }

inline fun <T: Any, R: Any> Try<Option<T>>.mapTryBlocking(crossinline f: (T) -> Option<R>): Try<Option<R>> =
    this.flatMapRight { f(it) }

suspend inline fun <T: Any, R: Any> Try<Option<T>>.mapTry(crossinline f: suspend (T) -> Option<R>): Try<Option<R>> =
    this.flatMapRight { f(it) }

inline fun <T: Any, R: Any> Try<Option<T>>.mapTryOptionBlocking(crossinline f: (T) -> R): Try<Option<R>> =
    this.flatMapRight { f(it).option }

suspend inline fun <T: Any, R: Any> Try<Option<T>>.mapTryOption(crossinline f: suspend (T) -> R): Try<Option<R>> =
    this.flatMapRight { f(it).option }

fun <T: Any, R: Any> Try<Option<T>>.zipWithTryOption(other: Try<Option<R>>): Try<Option<NTuple2<T, R>>> =
    this.flatMapTryOptionBlocking { t ->
        other.mapTryOptionBlocking { r -> t then r }
    }

fun <T1: Any, T2: Any, T3: Any> zipTryOption(
    t1: Try<Option<T1>>,
    t2: Try<Option<T2>>,
    t3: Try<Option<T3>>
): Try<Option<NTuple3<T1, T2, T3>>> =
    t1.zipWithTryOption(t2)
        .zipWithTryOption(t3)
        .mapTryOptionBlocking { (t1t2, t3) -> t1t2.then(t3) }

inline fun <reified T, reified R> Option<T>.cast(): Option<R> =
    this.flatMap { (it as? R).option }

inline fun <reified T, reified R> T.cast(): Option<R> =
    (this as? R).option
