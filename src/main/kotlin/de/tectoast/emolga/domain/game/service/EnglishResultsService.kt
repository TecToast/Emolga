package de.tectoast.emolga.domain.game.service

import de.tectoast.emolga.domain.config.model.GuildConfigType
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.features.various.config.K18n_EnglishResults
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single
class EnglishResultsService(private val configRepo: GuildConfigRepository) {
    suspend fun toggle(guild: Long): K18nMessage {
        return if (configRepo.toggle(guild, GuildConfigType.EnglishResults)) {
            K18n_EnglishResults.English
        } else {
            K18n_EnglishResults.German
        }
    }
}
