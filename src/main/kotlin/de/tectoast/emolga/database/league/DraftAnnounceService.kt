package de.tectoast.emolga.database.league

import de.tectoast.emolga.database.exposed.TierlistMeta
import de.tectoast.emolga.database.exposed.TierlistPriceConfigDispatcher
import de.tectoast.emolga.database.exposed.ValidationRelevantData
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.EmptyMessage
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.invoke
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single
class DraftAnnounceService(
    val priceConfigDispatcher: TierlistPriceConfigDispatcher,
    val banRoundConfigDispatcher: BanRoundConfigDispatcher,
    val timerService: DraftTimerService
) {
    suspend fun generateAnnounceData(
        idx: Int,
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
            val normalSnippet = with(ValidationRelevantData(picks, idx, ctx.league.teamSize)) {
                priceConfigDispatcher.buildAnnounceData(meta.priceManager, picks)
            }
            if (normalSnippet != null) {
                snippets += normalSnippet
            }
        }
        val basePart = b {
            snippets.joinToString(prefix = " (", postfix = ")") { it() }
        }
        if(!withTimerAnnounce) return if(snippets.isEmpty()) EmptyMessage else basePart
        val timerPart = timerService.getCurrentTimerMessage(ctx, idx)
        return b {
            "${basePart()} — ${timerPart()}"
        }
    }
}