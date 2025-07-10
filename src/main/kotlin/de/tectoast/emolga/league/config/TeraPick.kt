package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

@Serializable
data class TeraPickConfig(val tlIdentifier: String? = "TERA")

@Serializable
data class TeraPickData(val alreadyHasTeraUser: MutableSet<Int> = mutableSetOf())
