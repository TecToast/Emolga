package de.tectoast.emolga.domain.league.draft.service.util

import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.service.core.ban.BanRoundConfigDispatcher
import de.tectoast.emolga.domain.league.draft.service.timer.DraftTimerService
import de.tectoast.emolga.domain.league.tierlist.model.TierlistMeta
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierlistActionDispatcher
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.EmptyMessage
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.invoke
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single
class DraftAnnounceService(
    private val tierlistActionDispatcher: TierlistActionDispatcher,
    private val banRoundConfigDispatcher: BanRoundConfigDispatcher,
    private val timerService: DraftTimerService
) {
    suspend fun generateAnnounceData(
        picks: List<DraftPokemon>,
        withTimerAnnounce: Boolean,
        ctx: DraftRunContext,
        meta: TierlistMeta
    ): K18nMessage {
        val snippets = mutableListOf<K18nMessage>()
        val banSnippet = ctx.config.draftBan?.banRounds?.get(ctx.league.round)?.let {
            K18n_League.PossibleTiersToBan(
                banRoundConfigDispatcher.getPossibleBanTiers(it, ctx.league.alreadyBannedMonsThisRound)
                    .joinToString { s -> "**$s**" })
        }
        if (banSnippet != null) {
            snippets += banSnippet
        } else {
            val normalSnippet = tierlistActionDispatcher.buildAnnounceData(meta.config, picks)
            if (normalSnippet != null) {
                snippets += normalSnippet
            }
        }
        val basePart = b {
            snippets.joinToString(prefix = " (", postfix = ")") { it() }
        }
        if (!withTimerAnnounce) return if (snippets.isEmpty()) EmptyMessage else basePart
        val timerConfig = ctx.config.timer ?: return basePart
        val timerPart = timerService.getCurrentTimerMessage(timerConfig, ctx.league.draftData.timer)
        return b {
            "${basePart()} — ${timerPart()}"
        }
    }
}
