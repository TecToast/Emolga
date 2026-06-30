package de.tectoast.emolga.utils

fun Iterable<Long>.joinToTeammates() = joinToString(" & ") { "<@$it>" }
