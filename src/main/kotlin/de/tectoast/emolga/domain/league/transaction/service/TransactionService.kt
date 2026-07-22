package de.tectoast.emolga.domain.league.transaction.service

import de.tectoast.emolga.di.TransactionRunner
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.draft.model.config.TeraPickConfig
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.tierlist.model.config.PointBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierlistActionDispatcher
import de.tectoast.emolga.domain.league.transaction.model.APITransactionData
import de.tectoast.emolga.domain.league.transaction.model.TransactionEntry
import de.tectoast.emolga.domain.league.transaction.model.TransactionRequestData
import de.tectoast.emolga.domain.league.transaction.repository.TransactionCodesRepository
import de.tectoast.emolga.domain.league.transaction.repository.TransactionDataRepository
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class TransactionService(
    private val codesRepo: TransactionCodesRepository,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val leaguePickRepo: LeaguePickRepository,
    private val tierlistRepo: TierlistRepository,
    private val transactionDataRepo: TransactionDataRepository,
    private val pokemonDisplayService: PokemonDisplayService,
    private val repeatTaskScheduler: RepeatTaskScheduler,
    private val tierlistActionDispatcher: TierlistActionDispatcher,
    private val tx: TransactionRunner,
    private val successfulTransactionHandler: SuccessfulTransactionHandler
) {
    suspend fun getTransactionData(transactionId: Uuid): APITransactionData? {
        val (leagueName, idx) = codesRepo.getDataByCode(transactionId) ?: return null
        val config = leagueConfigRepo.getConfig(leagueName)
        val leagueData = leagueCoreRepo.getScalarLeagueDataOrNull(leagueName) ?: return null
        val maxTransactionPoints = config.transaction?.maxPoints ?: return null
        val guild = leagueData.guild
        val teraPickConfig = config.teraPick
        val allMons =
            tierlistRepo.getAllPokemonWithTeraTier(guild, config.tlIdentifier, teraPickConfig?.tlIdentifier ?: "TERA")
        val lookup = allMons.associateBy { it.showdownId }
        val alreadyPicked = leaguePickRepo.getAllPickedIds(leagueName)
        val myPicks = leaguePickRepo.getPicksForUser(leagueName, idx).filter { !it.quit }.map {
            val data = lookup[it.showdownId] ?: error("Could not find ${it.showdownId} in tierlist")
            data.copy(tera = it.tera)
        }
        val meta = tierlistRepo.getMeta(guild, config.tlIdentifier) ?: return null
        val tierlistConfig = meta.config
        val monMaxPoints = (tierlistConfig as? PointBasedTierlistConfig)?.globalPoints
        val transactionPoints =
            transactionDataRepo.getTransactionAmounts(leagueName, idx).remaining(maxTransactionPoints)
        val displayNames = pokemonDisplayService.getDisplayNames(allMons.map { it.showdownId }, guild, meta.language)
        return APITransactionData(
            picked = myPicks,
            available = allMons.map { it.copy(picked = alreadyPicked.contains(it.showdownId)) },
            teraCount = teraPickConfig?.amount ?: 0,
            teraMaxPoints = teraPickConfig?.maxPoints,
            monMaxPoints = monMaxPoints,
            transactionPoints = transactionPoints,
            maxTransactionPoints = maxTransactionPoints,
            displayNames = displayNames
        )
    }

    suspend fun submitTransaction(transactionId: Uuid, data: TransactionRequestData): Unit? = tx {
        if (data.picks.size != data.drops.size) return@tx null
        val (leagueName, idx) = codesRepo.getDataByCode(transactionId) ?: return@tx null
        val leagueData = leagueCoreRepo.getScalarLeagueDataOrNull(leagueName) ?: return@tx null
        val myPicks = leaguePickRepo.getPicksForUser(leagueName, idx).toMutableList()
        val alreadyPicked = leaguePickRepo.getAllPickedIds(leagueName)
        val myPickIds = myPicks.filter { !it.quit }.map { it.showdownId }
        val config = leagueConfigRepo.getConfig(leagueName)
        val transactionConfig = config.transaction ?: return@tx null
        val teraConfig = config.teraPick ?: TeraPickConfig(amount = 0, maxPoints = 0)
        if (data.teraUsers.size != teraConfig.amount) return@tx null
        if (data.picks.any { it in alreadyPicked }) return@tx null
        if (!myPickIds.containsAll(data.drops)) return@tx null
        val resultingPickIds = myPickIds - data.drops + data.picks
        if (!resultingPickIds.containsAll(data.teraUsers)) return@tx null
        val amounts = transactionDataRepo.getTransactionAmounts(leagueName, idx)
        val week =
            repeatTaskScheduler.getUpcomingNumber(RepeatTaskType.TransactionDocInsert(leagueName)) ?: return@tx null
        val currentEntry = transactionDataRepo.getRunning(leagueName, week)[idx] ?: TransactionEntry()
        val currentTeraUsers = myPicks.filter { !it.quit && it.tera }.map { it.showdownId }.toSet()
        if (data.picks.isEmpty() && currentTeraUsers == data.teraUsers) return@tx null // No changes
        val newTeraUsers = data.teraUsers - currentTeraUsers
        val oldTeraUsers = currentTeraUsers - data.teraUsers
        val teraUserDiscount = data.drops.intersect(currentTeraUsers).size
        val newTransactionPoints =
            amounts.remaining(transactionConfig.maxPoints) - data.drops.size - newTeraUsers.size + teraUserDiscount
        if (newTransactionPoints < 0) return@tx null
        val dropsAsList = data.drops.toList()
        val picksAsList = data.picks.toList()
        val tlMeta = tierlistRepo.getMeta(leagueData.guild, config.tlIdentifier) ?: return@tx null
        val tierlistConfig = tlMeta.config
        dropsAsList.forEachIndexed { index, drop ->
            val old = myPicks.firstOrNull { it.showdownId == drop } ?: return@tx null
            val newName = picksAsList[index]
            val newTier = tierlistRepo.getTier(leagueData.guild, config.tlIdentifier, newName) ?: return@tx null
            old.showdownId = newName
            old.tier = newTier
            myPicks += DraftPokemon(drop, tier = "N/A", quit = true)
        }
        myPicks.forEach {
            if (!it.quit) it.tera = data.teraUsers.contains(it.showdownId)
        }
        with(ValidationRelevantData(myPicks, idx, tlMeta.teamSize)) {
            val error = tierlistActionDispatcher.checkLegalityOfQueue(tierlistConfig, idx, currentState = emptyList())
            if (error != null) return@tx null
        }
        leaguePickRepo.storeNewPickList(leagueData.guild, leagueName, idx, myPicks)
        currentEntry.picks += (data.picks - currentEntry.picks.toSet())
        currentEntry.drops += (data.drops - currentEntry.drops.toSet())
        amounts.mons += data.picks.size
        amounts.extraTeras += newTeraUsers.size - teraUserDiscount
        transactionDataRepo.setTransactionAmounts(leagueName, idx, amounts)
        transactionDataRepo.setRunning(leagueName, week, idx, currentEntry.drops, currentEntry.picks)
        codesRepo.deleteCode(transactionId)
        successfulTransactionHandler.handleSuccessfulTransaction(
            idx,
            leagueData,
            data,
            week,
            newTransactionPoints,
            oldTeraUsers.zip(newTeraUsers)
        )
    }


}