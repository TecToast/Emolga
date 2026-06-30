package de.tectoast.emolga.domain.guildspecific.flegmon.dsb.model

import kotlinx.serialization.Serializable

@Serializable
data class DSBUser(val id: String, val name: String, val avatar: String)