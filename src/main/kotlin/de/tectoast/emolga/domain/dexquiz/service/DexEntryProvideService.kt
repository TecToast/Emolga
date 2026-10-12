package de.tectoast.emolga.domain.dexquiz.service

import de.tectoast.emolga.domain.dexquiz.repository.DexEntryRepository
import org.koin.core.annotation.Single

@Single
class DexEntryProvideService(private val repo: DexEntryRepository, private val redactService: DexEntryRedactService) {
    suspend fun getDexEntryById(id: Int) =
        repo.getDexEntryById(id)?.let { it.copy(entry = redactService.redactDexEntryText(it.showdownId, it.entry)) }

    suspend fun getRandomDexEntry() =
        repo.getRandomDexEntry()?.let { it.copy(entry = redactService.redactDexEntryText(it.showdownId, it.entry)) }
}