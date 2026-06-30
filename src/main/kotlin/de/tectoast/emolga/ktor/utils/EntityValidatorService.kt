package de.tectoast.emolga.ktor.utils

import de.tectoast.emolga.discord.DiscordEntityValidator
import org.koin.core.annotation.Single
import kotlin.reflect.full.memberProperties

@Single
class EntityValidatorService(private val discordEntityValidator: DiscordEntityValidator) {
    suspend fun validate(data: Any): Boolean {
        for (property in data::class.memberProperties) {
            val value = property.call(data) ?: continue
            if (value is Long) {
                val longType = property.annotations.findConfig()?.longType ?: continue
                val result = when (longType) {
                    LongType.CHANNEL -> discordEntityValidator.validateChannelId(value)
                    LongType.ROLE -> discordEntityValidator.validateRoleId(value)
                    else -> {
                        true
                    }
                }
                if (!result) return false
            } else if (value::class.isData) {
                if (!validate(value)) return false
            }
        }
        return true
    }
}
