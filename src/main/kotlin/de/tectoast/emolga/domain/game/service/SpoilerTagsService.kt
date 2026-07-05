package de.tectoast.emolga.domain.game.service

import de.tectoast.emolga.domain.config.model.GuildConfigType
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import org.koin.core.annotation.Single

@Single
class SpoilerTagsService(private val configRepo: GuildConfigRepository) {
    suspend fun toggle(guild: Long): Boolean {
        return configRepo.toggle(guild, GuildConfigType.SpoilerTags)
    }
}