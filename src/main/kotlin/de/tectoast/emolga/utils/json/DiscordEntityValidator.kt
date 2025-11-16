package de.tectoast.emolga.utils.json

import de.tectoast.emolga.bot.jda

object DiscordEntityValidator {
    fun validateChannelId(id: String): Boolean = jda.getTextChannelById(id) != null

    fun validateRoleId(id: String): Boolean = jda.getRoleById(id) != null
}