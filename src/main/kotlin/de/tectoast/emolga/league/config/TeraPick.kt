package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

@Serializable
data class TeraPickConfig(val messageOnIllegalPick: String = "Dieses Pokemon kann man nicht als TeraPick wählen!")

@Serializable
data class TeraPickData(val alreadyPaid: MutableMap<Int, Int> = mutableMapOf())