package de.tectoast.emolga.domain.guildspecific.flegmon.dsb.service

import de.tectoast.emolga.domain.guildspecific.flegmon.dsb.model.DSBMessage
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.repository.PokedexRepository
import de.tectoast.emolga.domain.pokemon.repository.PokemonNamesRepository
import org.koin.core.annotation.Single

@Single
class DSBSubmissionService(
    private val dsbFlow: DSBFlow,
    private val pokedexRepo: PokedexRepository,
    private val pokemonNamesRepo: PokemonNamesRepository,
) {

    suspend fun handleMonSubmission(userId: Long, showdownId: ShowdownID) {
        val rawData = pokemonNamesRepo.getRawNames(listOf(showdownId), guild = 0).first()
        dsbFlow.emit(
            DSBMessage(
                userId.toString(),
                rawData.nameDe + " / " + rawData.nameEn,
                "/api/emolga/monimg/SUGIMORI/${
                    pokedexRepo.get(showdownId)!!.calcSpriteName()
                }.png",
                System.currentTimeMillis().toHexString()
            )
        )
    }

    suspend fun handleTextSubmission(userId: Long, text: String) {
        dsbFlow.emit(DSBMessage(userId.toString(), text, timestamp = System.currentTimeMillis().toHexString()))
    }
}