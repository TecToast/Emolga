package de.tectoast.emolga.domain.game.service

import de.tectoast.emolga.domain.game.repository.SpoilerTagsRepository
import org.koin.core.annotation.Single

@Single
class SpoilerTagsService(private val repo: SpoilerTagsRepository) {
    suspend fun toggle(guild: Long): Boolean {
        if (repo.delete(guild)) {
            return false
        }
        repo.insert(guild)
        return true
    }
}