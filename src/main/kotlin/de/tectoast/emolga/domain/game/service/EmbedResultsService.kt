package de.tectoast.emolga.domain.game.service

import de.tectoast.emolga.domain.game.repository.EmbedResultsRepository
import org.koin.core.annotation.Single

@Single
class EmbedResultsService(private val repo: EmbedResultsRepository) {
    suspend fun toggle(guild: Long): Boolean {
        if (repo.enableEmbedResults(guild)) {
            return true
        }
        repo.disableEmbedResults(guild)
        return false
    }
}