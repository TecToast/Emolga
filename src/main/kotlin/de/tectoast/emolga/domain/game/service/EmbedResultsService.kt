package de.tectoast.emolga.domain.game.service

import de.tectoast.emolga.domain.config.model.GuildConfigType
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.features.various.config.K18n_EmbedResults
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single
class EmbedResultsService(private val configRepo: GuildConfigRepository) {
    suspend fun toggle(guild: Long): K18nMessage {
        return if (configRepo.toggle(guild, GuildConfigType.EmbedResults)) {
            K18n_EmbedResults.Enabled
        } else {
            K18n_EmbedResults.Disabled
        }
    }
}