package de.tectoast.emolga.domain.guildspecific.flegmon.dsb.model

import kotlinx.serialization.Serializable

@Serializable
data class DSBConfig(val host: Long, val guild: Long, val categories: List<String>, val users: List<Long>)