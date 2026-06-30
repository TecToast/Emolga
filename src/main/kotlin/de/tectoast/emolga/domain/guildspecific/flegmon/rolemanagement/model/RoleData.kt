package de.tectoast.emolga.domain.guildspecific.flegmon.rolemanagement.model

data class RoleData(
    val compId: String, val name: String, val description: String, val roleId: Long, val formattedEmoji: String? = null
)