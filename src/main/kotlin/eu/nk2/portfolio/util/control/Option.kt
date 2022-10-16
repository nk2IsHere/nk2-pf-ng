package eu.nk2.portfolio.util.control

typealias Option<T> = Either<Unit, T>

val <T: Any> T?.option: Option<T>
    get() = if(this == null) Either.Left(Unit)
        else Either.Right(this)

val <T: Any> T.something: Option<T>
    get() = Either.Right(this)

val <T> T.nothing: Option<T>
    get() = Either.Left(Unit)

fun <T> nothing(): Option<T> =
    Either.Left(Unit)
