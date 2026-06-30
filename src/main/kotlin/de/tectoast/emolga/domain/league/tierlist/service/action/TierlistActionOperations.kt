package de.tectoast.emolga.domain.league.tierlist.service.action

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.DraftActionContext
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.queue.model.QueuedAction
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.k18n.generated.K18nMessage

interface TierlistActionOperations<C : TierlistConfig> {
    fun publicTierToDBTier(config: C, tier: String): String
    fun compareTiers(config: C, tierA: String, tierB: String): Int?

    context(data: ValidationRelevantData)
    suspend fun handleDraftActionWithGeneralChecks(
        config: C, action: DraftAction, context: DraftActionContext? = null
    ): ErrorOrNull

    suspend fun buildAnnounceData(config: C, picks: List<DraftPokemon>): K18nMessage?
    fun getTiers(config: C): List<String>
    fun getTiersForUpdraftCompare(config: C): List<String>

    context(data: ValidationRelevantData)
    suspend fun checkLegalityOfQueue(config: C, idx: Int, currentState: List<QueuedAction>): ErrorOrNull

    fun getTierOrderingComparatorWithoutName(config: C): Comparator<DraftPokemon>

    fun getSortedPicks(config: C, picks: List<DraftPokemon>): List<DraftPokemon>
}