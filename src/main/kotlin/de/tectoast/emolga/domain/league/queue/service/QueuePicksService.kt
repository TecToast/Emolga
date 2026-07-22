package de.tectoast.emolga.domain.league.queue.service

import de.tectoast.emolga.database.exposed.*
import de.tectoast.emolga.di.TransactionRunner
import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.core.model.DraftState
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.queue.model.DroppedMon
import de.tectoast.emolga.domain.league.queue.model.QueuedAction
import de.tectoast.emolga.domain.league.queue.model.QueuedMon
import de.tectoast.emolga.domain.league.queue.repository.QueuedPicksRepository
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.tierlist.service.core.TierDataService
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.domain.statestore.service.QueuePicksStateStoreHandler
import de.tectoast.emolga.domain.statestore.service.StateStoreDispatcher
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.features.league.draft.generic.K18n_NoTierlist
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.*
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class QueuePicksService(
    private val leagueCoreRepository: LeagueCoreRepository,
    private val queuedPicksRepository: QueuedPicksRepository,
    private val pickRepository: LeaguePickRepository,
    private val tierlistRepository: TierlistRepository,
    private val tierDataService: TierDataService,
    private val displayService: PokemonDisplayService,
    private val queueValidationService: QueueValidationService,
    private val tx: TransactionRunner
) : KoinComponent {
    private val stateStore: StateStoreDispatcher by inject()
    suspend fun addAction(
        guild: Long,
        leagueName: String,
        config: LeagueConfig,
        idx: Int,
        user: Long,
        mon: ShowdownID,
        oldMon: ShowdownID?,
        requestedTier: String?
    ): K18nMessageOrError {
        config.checkQueueEnabled()?.let { return it.error() }
        val tl = tierlistRepository.getMeta(guild, config.tlIdentifier) ?: return K18n_NoTierlist.error()
        suspend fun display(showdownId: ShowdownID) = displayService.getDisplayName(showdownId, guild, tl.language)
        return tx {
            val draftData = leagueCoreRepository.getDraftStateLocking(leagueName)
            val ownPicks = pickRepository.getPicksForUser(leagueName, idx)
            val allPicks = pickRepository.getAllPickedIds(leagueName)
                .apply { addAll(draftData.draftBan.bannedMons.values.flatten().map { it.showdownId }) }
            if (oldMon == null && draftData.draftState == DraftState.OFF && ownPicks.isNotEmpty() && !config.triggers.allowPickDuringSwitch) {
                return@tx K18n_QueuePicks.OnlyWithSwitchAllowed.error()
            }
            val oldMonAsDropped = oldMon?.let { pokemon ->
                ownPicks.firstOrNull { it.showdownId == pokemon }?.let { DroppedMon(pokemon, it.tier) }
                    ?: return@tx K18n_QueuePicks.PokemonNotInTeam(
                        display(oldMon)
                    ).error()
            }
            if (mon in allPicks) {
                return@tx K18n_QueuePicks.PokemonAlreadyPicked(display(mon))
                    .error()
            }
            val data = queuedPicksRepository.getSingle(leagueName, idx)
            for (q in data.queued) {
                if (q.g.id == mon) return@tx K18n_QueuePicks.PokemonInYourQueue(
                    display(mon)
                ).error()
                if (oldMon != null && q.y?.id == oldMon) return@tx K18n_QueuePicks.OldMonInYourQueue(
                    display(oldMon)
                ).error()
            }
            val tierData =
                tierDataService.getTierData(tl, mon, requestedTier).getOrReturn<_, K18nMessage> { return@tx it }
            val queuedAction =
                QueuedAction(
                    QueuedMon(mon, tierData.specified, tierSpecified = tierData.isTierSpecified && tierData.specified != tierData.official),
                    oldMonAsDropped
                )
            val newlist = data.queued.toMutableList().apply { add(queuedAction) }
            queueValidationService.validateQueue(leagueName, idx, tl.teamSize, tl.config, newlist)
                ?.let { return@tx it.error() }
            stateStore.processIgnoreMissing<_, QueuePicksStateStoreHandler>(user) {
                addNewMon(queuedAction)
            }
            data.queued = newlist
            data.enabled = false
            queuedPicksRepository.updateSingle(leagueName, idx, data)
            K18n_QueuePicks.AddSuccess(buildString {
                if (oldMon != null) {
                    append("`")
                    append(display(oldMon))
                    append("` -> ")
                }
                append("`")
                append(display(mon))
                append("`")
            }).success()
        }
    }

    suspend fun setNewState(
        leagueName: String,
        guild: Long,
        idx: Int,
        currentState: List<QueuedAction>,
        enable: Boolean
    ): ErrorOrNull = tx {
        leagueCoreRepository.getDraftStateLocking(leagueName)
        queueValidationService.validateQueue(leagueName, guild, idx, currentState)
            ?.let { return@tx it }
        val data = queuedPicksRepository.getSingle(leagueName, idx)
        data.queued = currentState.toMutableList()
        data.enabled = enable
        queuedPicksRepository.updateSingle(leagueName, idx, data)
        null
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
            queueValidationService.validateQueue(leagueName, idx, tl.teamSize, tl.config, data.queued)
                ?.let { return@tx it.error() }
            data.enabled = enable
            queuedPicksRepository.updateSingle(leagueName, idx, data)
            (if (enable) K18n_QueuePicks.QueueEnabled else K18n_QueuePicks.QueueDisabled).success()
        }
    }

    suspend fun toggleSuccessfulNotification(
        guild: Long,
        leagueName: String,
        idx: Int,
        config: LeagueConfig
    ): K18nMessageOrError {
        config.checkQueueEnabled()?.let { return it.error() }
        return tx {
            leagueCoreRepository.getDraftStateLocking(leagueName)
            val data = queuedPicksRepository.getSingle(leagueName, idx)
            data.notifyOnSuccess = !data.notifyOnSuccess
            queuedPicksRepository.updateSingle(leagueName, idx, data)
            (if (data.notifyOnSuccess) K18n_QueuePicks.ToggleSuccessfulPingEnable else K18n_QueuePicks.ToggleSuccessfulPingDisable).success()
        }
    }

    private fun LeagueConfig.checkQueueEnabled(): ErrorOrNull {
        return if (!triggers.queuePicks) {
            K18n_QueuePicks.Disabled
        } else null
    }
}
