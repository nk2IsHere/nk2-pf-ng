
package eu.nk2.portfolio.util.control

typealias Try<T> = Either<Throwable, T>

fun <T> Try<T>.ignoreError(value: T): Try<T> {
    return if (this.isRight) this as Either.Right<T> else Either.Right(value)
}

fun Try<Unit>.ignoreError(): Try<Unit> = ignoreError(Unit)

@Suppress("FunctionName")
suspend fun <T> Try(block: suspend () -> T): Try<T> {
    return try {
        Either.Right(block())
    } catch (e: Throwable) {
        Either.Left(e)
    }
}

@Suppress("FunctionName")
fun <T> TryBlocking(block: () -> T): Try<T> {
    return try {
        Either.Right(block())
    } catch (e: Throwable) {
        Either.Left(e)
    }
}

fun <T> Try<T>.unwrap(): T {
    return if (this.isRight) {
        this.asRight().value
    } else {
        throw this.asLeft().value
    }
}

val <T> T.wrap: Try<T>
    get() = Either.Right(this)

fun <T, E: Throwable> E.rewrap(): Try<T> =
    Either.Left(this)
