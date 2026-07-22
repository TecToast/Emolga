package de.tectoast.emolga.domain.league.tierlist.model

import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.utils.Language

data class TierlistMeta(
    val guild: Long,
    val identifier: String,
    val language: Language,
    val teamSize: Int,
    val config: TierlistConfig
)