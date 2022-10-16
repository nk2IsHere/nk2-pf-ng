package eu.nk2.portfolio.util.control

import eu.nk2.portfolio.util.control.Either.Left
import eu.nk2.portfolio.util.control.Either.Right


sealed class Either<out L, out R> {
    data class Right<out T>(val value: T) : Either<Nothing, T>()
    data class Left<out T>(val value: T) : Either<T, Nothing>()
}

fun <L, R> either(left: L? = null, right: R? = null): Either<L, R> =
    when {
        left == null && right == null || left != null && right != null -> error("Do not use either like that!")
        left != null -> Left(left)
        right != null -> Right(right)
        else -> error("Impossible case")
    }

fun <L> Either<L, *>.asLeft() = this as Left<L>
val Either<*, *>.isLeft: Boolean
    get() = this is Left<*>

fun <R> Either<*, R>.asRight() = this as Right<R>
val Either<*, *>.isRight: Boolean
    get() = this is Right<*>

inline fun <L, R, T> Either<L, R>.fold(left: (L) -> T, right: (R) -> T): T {
    return when (this) {
        is Left -> left(value)
        is Right -> right(value)
    }
}

inline fun <L, R> Either<L, R>.foldRight(left: (L) -> R): R {
    return when (this) {
        is Left -> left(value)
        is Right -> value
    }
}

inline fun <L, R> Either<L, R>.foldLeft(right: (R) -> L): L {
    return when (this) {
        is Left -> value
        is Right -> right(value)
    }
}

@JvmName("foldL1R1R")
inline fun <L1, R1, R, T> Either<Either<L1, R1>, R>.fold(left1: (L1) -> T, right1: (R1) -> T, right: (R) -> T): T {
    return when (this) {
        is Left<Either<L1, R1>> -> when(value) {
            is Left -> left1(value.value)
            is Right -> right1(value.value)
        }
        is Right -> right(value)
    }
}

@JvmName("foldLL1R1")
inline fun <L, L1, R1, T> Either<L, Either<L1, R1>>.fold(left: (L) -> T, left1: (L1) -> T, right1: (R1) -> T): T {
    return when (this) {
        is Left<L> -> left(value)
        is Right<Either<L1, R1>> -> when(value) {
            is Left -> left1(value.value)
            is Right -> right1(value.value)
        }
    }
}

inline fun <L, R, T> Either<L, R>.flatMap(f: (R) -> Either<L, T>): Either<L, T> {
    return fold({ this as Left }, f)
}

inline fun <L, L1, R1, T> Either<L, Either<L1, R1>>.flatMapRight(f: (R1) -> Either<L1, T>): Either<L, Either<L1, T>> {
    return map { it.flatMap(f) }
}

inline fun <L1, R1, R, T> Either<Either<L1, R1>, R>.flatMapLeft(f: (R1) -> Either<L1, T>): Either<Either<L1, T>, R> {
    return mapLeft { it.flatMap(f) }
}

inline fun <L, R, T> Either<L, R>.map(f: (R) -> T): Either<L, T> {
    return flatMap { Right(f(it)) }
}

inline fun <L, R, T> Either<L, R>.mapLeft(f: (L) -> T): Either<T, R> {
    return fold({ Left(f(it)) }, { this as Right })
}

fun <L, L1, R1> Either<L, Either<L1, R1>>.popLeft(): Either<L1, Either<L, R1>> {
    return fold(
        { Right(Left(it)) },
        { Left(it) },
        { Right(Right(it)) }
    )
}

@JvmName("join")
fun <L, R> List<Either<L, R>>.join(): Either<L, List<R>> {
    if (isEmpty()) return Right(emptyList())
    val initial = first().map { mutableListOf(it) }
    return (
        if (size == 1) initial
        else {
            drop(1)
                .fold(initial) { acc, either ->
                    acc.flatMap { list -> either.map { list.add(it); list } }
                }
        }
    )
}

fun <L> Either<L, L>.zip() =
    when(this) {
        is Left -> this.value
        is Right -> this.value
    }

inline fun <L, R> Either<L, R>.zip(other: Either<L, R>, zip: (R, R) -> R): Either<L, R> {
    return this.flatMap { it1 ->
        other.map { it2 ->
            zip(it1, it2)
        }
    }
}

@JvmName("join")
fun <L, R> Either<L, R>.join(other: Either<L, R>): Either<L, List<R>> {
    return listOf(this, other).join()
}

fun <L> Either<L, *>.ignoreRight(): Either<L, Unit> {
    return if (this.isLeft) this as Left<L> else Right(Unit)
}

fun <L, R> Either<L, R>.defaultRight(right: R): Right<R> {
    return if (this.isRight) this as Right<R> else Right(right)
}
