package de.tectoast.emolga.discord

import net.dv8tion.jda.api.JDA

sealed interface OptionalJDA {
    data class Present(val jda: JDA) : OptionalJDA
    object Absent : OptionalJDA
}

val OptionalJDA.jdaOrNull: JDA?
    get() = when (this) {
        is OptionalJDA.Present -> jda
        is OptionalJDA.Absent -> null
    }