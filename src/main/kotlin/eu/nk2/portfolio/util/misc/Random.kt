package eu.nk2.portfolio.util.misc

import java.util.*
import kotlin.streams.asSequence

const val ALPHABET_AND_NUMBERS = "1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM"

fun String.randomOf(length: Int) =
    Random().ints(length.toLong(), 0, this.length)
        .asSequence()
        .map(this::get)
        .joinToString("")
