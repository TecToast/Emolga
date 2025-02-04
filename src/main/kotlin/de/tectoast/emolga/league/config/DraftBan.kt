package de.tectoast.emolga.league.config

import de.tectoast.emolga.league.BanRoundConfig
import de.tectoast.emolga.league.BanSkipBehavior
import de.tectoast.emolga.utils.draft.DraftPokemon
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class DraftBanConfig(
    val banRounds: Map<Int, BanRoundConfig> = mapOf(),
    val notBannable: Set<String> = setOf(),
    val skipBehavior: BanSkipBehavior = BanSkipBehavior.NOTHING
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DraftBanData(
    @EncodeDefault
    val bannedMons: MutableMap<Int, MutableSet<DraftPokemon>> = mutableMapOf()
)