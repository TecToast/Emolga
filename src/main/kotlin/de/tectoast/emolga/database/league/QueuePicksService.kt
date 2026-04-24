package de.tectoast.emolga.database.league

import de.tectoast.emolga.database.exposed.QueuePicksStateHandler
import de.tectoast.emolga.database.exposed.QueuedAction
import de.tectoast.emolga.database.exposed.QueuedMon
import de.tectoast.emolga.database.exposed.QueuedPicksRepository
import de.tectoast.emolga.database.exposed.StateStoreDispatcher
import de.tectoast.emolga.database.exposed.TierlistPriceConfig
import de.tectoast.emolga.database.exposed.TierlistPriceConfigDispatcher
import de.tectoast.emolga.database.exposed.TierlistRepository
import de.tectoast.emolga.database.exposed.TierlistService
import de.tectoast.emolga.database.exposed.ValidationRelevantData
import de.tectoast.emolga.database.league.GuildDefaultConfigTable.config
import de.tectoast.emolga.di.TransactionRunner
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.features.league.draft.generic.K18n_NoTierlist
import de.tectoast.emolga.features.league.draft.generic.K18n_NotInTierlist
import de.tectoast.emolga.features.league.draft.isIllegal
import de.tectoast.emolga.league.DraftState
import de.tectoast.emolga.league.config.LeagueConfig
import de.tectoast.emolga.utils.json.ErrorOrNull
import de.tectoast.emolga.utils.json.K18nMessageOrError
import de.tectoast.emolga.utils.json.error
import de.tectoast.emolga.utils.json.getOrReturn
import de.tectoast.emolga.utils.json.success
import de.tectoast.emolga.utils.notNullPrepend
import de.tectoast.k18n.generated.K18nMessage
import sun.jvm.hotspot.HelloWorld.e

class QueuePicksService(
    val stateStore: StateStoreDispatcher,
    val leagueCoreRepository: LeagueCoreRepository,
    val queuedPicksRepository: QueuedPicksRepository,
    val pickRepository: LeaguePickRepository,
    val tierlistRepository: TierlistRepository,
    val priceConfigDispatcher: TierlistPriceConfigDispatcher,
    val tierlistService: TierlistService,
    val picksRepo: LeaguePickRepository,
    val tx: TransactionRunner
) {


    suspend fun addAction(
        guild: Long,
        leagueName: String,
        config: LeagueConfig,
        idx: Int,
        user: Long,
        mon: String,
        oldMon: String?,
        requestedTier: String?
    ): K18nMessageOrError {
        // TODO: Move validation logic to core validation layer
        config.checkQueueEnabled()?.let { return it.error() }
        val tl = tierlistRepository.getMeta(guild, config.tlIdentifier) ?: return K18n_NoTierlist.error()
        return tx {
            val draftData = leagueCoreRepository.getDraftStateLocking(leagueName)
            val ownPicks = pickRepository.getPicksForUser(leagueName, idx)
            val allPicks = pickRepository.getAllPickedIds(leagueName)
            if (oldMon == null && draftData.draftState == DraftState.OFF && ownPicks.isNotEmpty() && !config.triggers.allowPickDuringSwitch) {
                return@tx K18n_QueuePicks.OnlyWithSwitchAllowed.error()
            }
            if (oldMon != null && !ownPicks.any { it.name == oldMon }) {
                // TODO tl name
                return@tx K18n_QueuePicks.PokemonNotInTeam(oldMon).error()
            }
            if (mon in allPicks) {
                // TODO tl name
                return@tx K18n_QueuePicks.PokemonAlreadyPicked(mon).error()
            }
            val data = queuedPicksRepository.getSingle(leagueName, idx)
            for (q in data.queued) {
                if (q.g.id == mon) return@tx K18n_QueuePicks.PokemonInYourQueue(mon).error()
                if (oldMon != null && q.y == oldMon) return@tx K18n_QueuePicks.OldMonInYourQueue(
                    oldMon
                ).error()
            }
            val tierData =
                tierlistService.getTierData(tl, mon, requestedTier).getOrReturn<_, K18nMessage> { return@tx it }
            val queuedAction =
                QueuedAction(QueuedMon(mon, tierData.specified, tierSpecified = tierData.isTierSpecified), oldMon)
            val newlist = data.queued.toMutableList().apply { add(queuedAction) }
            validateQueue(leagueName, idx, config.teamSize, tl.priceConfig, newlist)?.let { return@tx it.error() }
            stateStore.processIgnoreMissing<_, QueuePicksStateHandler>(user) {
                addNewMon(queuedAction)
            }
            data.queued = newlist
            data.enabled = false
            queuedPicksRepository.updateSingle(leagueName, idx, data)
            K18n_QueuePicks.AddSuccess("`${mon}`".notNullPrepend(oldMon) { "`${it}` -> " }).success()
        }
    }

    private suspend fun validateQueue(leagueName: String, idx: Int, teamSize: Int, priceConfig: TierlistPriceConfig, list: List<QueuedAction>) =
        with(ValidationRelevantData(picksRepo.getPicksForUser(leagueName, idx), idx, teamSize)) {
            priceConfigDispatcher.checkLegalityOfQueue(priceConfig, idx, list)
        }


    suspend fun changeActivation(
        enable: Boolean,
        guild: Long,
        leagueName: String,
        idx: Int,
        config: LeagueConfig
    ): K18nMessageOrError {
        config.checkQueueEnabled()?.let { return it.error() }
        return tx {
            leagueCoreRepository.getDraftStateLocking(leagueName)
            val data = queuedPicksRepository.getSingle(leagueName, idx)
            val tl = tierlistRepository.getMeta(guild, config.tlIdentifier) ?: return@tx K18n_NoTierlist.error()
            validateQueue(leagueName, idx, config.teamSize, tl.priceConfig, data.queued)?.let { return@tx it.error() }
            data.enabled = enable
            queuedPicksRepository.updateSingle(leagueName, idx, data)
            (if (enable) K18n_QueuePicks.QueueEnabled else K18n_QueuePicks.QueueDisabled).success()
        }
    }
}

fun LeagueConfig.checkQueueEnabled(): ErrorOrNull {
    return if (!triggers.queuePicks) {
        K18n_QueuePicks.Disabled
    } else null
}
