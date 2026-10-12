package de.tectoast.emolga.domain.dexquiz.service

import de.tectoast.emolga.domain.dexquiz.model.DexEntry
import org.koin.core.annotation.Single

@Single
class DexEntryShowService(private val provideService: DexEntryProvideService) {
    val idToEntryCache = mutableMapOf<Int, DexEntry?>()
    suspend fun generateNewDexEntry(): DexEntry? {
        val newEntry = provideService.getRandomDexEntry() ?: return null
        idToEntryCache[newEntry.id] = newEntry
        return newEntry
    }

    suspend fun lookupDexEntryById(id: Int): DexEntry? {
        return idToEntryCache.getOrPut(id) { provideService.getDexEntryById(id) }
    }
}