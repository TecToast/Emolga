package de.tectoast.emolga.domain.game.service

import de.tectoast.emolga.domain.game.repository.EnglishResultsRepository
import de.tectoast.emolga.features.various.config.K18n_EnglishResults
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single
class EnglishResultsService(private val englishResultsRepo: EnglishResultsRepository) {
    suspend fun toggle(guild: Long): K18nMessage {
        if (englishResultsRepo.delete(guild)) {
            return K18n_EnglishResults.German
        }
        englishResultsRepo.insert(guild)
        return K18n_EnglishResults.English
    }
}
