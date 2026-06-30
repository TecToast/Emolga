package de.tectoast.emolga.di

sealed interface OptionalString {
    data class Present(val value: String) : OptionalString
    object Absent : OptionalString
}

val OptionalString.valueOrNull: String?
    get() = when (this) {
        is OptionalString.Present -> value
        is OptionalString.Absent -> null
    }