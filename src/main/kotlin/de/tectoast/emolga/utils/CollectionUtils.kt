package de.tectoast.emolga.utils

inline fun <T> Collection<T>.filterStartsWithIgnoreCase(other: String, tostring: (T) -> String = { it.toString() }) =
    mapNotNull {
        val str = tostring(it)
        if (str.startsWith(other, ignoreCase = true)) str else null
    }

inline fun <T> Collection<T>.filterContainsIgnoreCase(other: String, tostring: (T) -> String = { it.toString() }) =
    mapNotNull {
        val str = tostring(it)
        if (str.contains(other, ignoreCase = true)) str else null
    }