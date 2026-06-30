package de.tectoast.emolga.features.league.draft.queue

import de.tectoast.emolga.domain.league.util.service.LeagueQueryService
import de.tectoast.emolga.features.interaction.InteractionData


context(iData: InteractionData)
internal suspend fun LeagueQueryService.byCommand() = getByGuildUser(iData.gid, iData.user)
