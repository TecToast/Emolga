package de.tectoast.emolga.domain.league.draft.model.core

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.core.model.DraftRelevantLeagueData
import de.tectoast.emolga.domain.league.tierlist.model.TierlistMeta

data class DraftRunContext(
    val league: DraftRelevantLeagueData,
    val config: LeagueConfig,
    val tierlistMeta: TierlistMeta,
    var pickContext: PickContext
) {
    val activeIdx: Int get() = pickContext.idx
}
