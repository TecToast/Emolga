package de.tectoast.emolga.domain.league.draft.model.ban

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable

@Serializable
data class DraftBanConfig(
    val banRounds: Map<Int, BanRoundConfig> = mapOf(),
    val notBannable: Set<ShowdownID> = setOf(),
    val skipBehavior: BanSkipBehavior = BanSkipBehavior.NOTHING
)
