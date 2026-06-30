package de.tectoast.emolga.domain.guildspecific.flegmon.dsb.model

import kotlinx.serialization.Serializable

@Serializable
data class DSBData(val users: List<DSBUser>, val categories: List<String>)