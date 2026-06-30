package de.tectoast.emolga.domain.league.draft.model.ban

import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DraftBanData(
    @EncodeDefault
    val bannedMons: MutableMap<Int, MutableSet<DraftPokemon>> = mutableMapOf()
)
