package de.tectoast.emolga.database.league

import de.tectoast.emolga.database.coord.AstEnvironment
import de.tectoast.emolga.database.exposed.*
import de.tectoast.emolga.di.TransactionRunner
import de.tectoast.emolga.features.league.draft.generic.K18n_NoTierlist
import de.tectoast.emolga.league.K18n_BanRoundConfig
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.league.TierData
import de.tectoast.emolga.league.config.LeagueConfig
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.K18n_DraftUtils
import de.tectoast.emolga.utils.json.*
import de.tectoast.k18n.generated.K18nMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass
import kotlin.reflect.cast

@Serializable
sealed interface DraftInput {
    val pokemon: DraftName
    val freesPokemon: String?
}

@Serializable
data class PickInput(
    override val pokemon: DraftName,
    val tier: String?,
    val free: Boolean,
    val tera: Boolean = false,
    val noCost: Boolean = false
) : DraftInput {
    override val freesPokemon = null
}

@Serializable
data class SwitchInput(val oldmon: String, override val pokemon: DraftName) : DraftInput {
    override val freesPokemon = oldmon

}

@Serializable
data class BanInput(override val pokemon: DraftName) : DraftInput {
    override val freesPokemon = null

}

enum class DraftMessageType {
    REGULAR, QUEUE, RANDOM, ACCEPT, REROLL
}


data class DraftRunContext(
    val league: DraftRelevantLeagueData, val config: LeagueConfig, var activeIdx: Int
)

@Single
class DraftService(
    val tx: TransactionRunner,
    val leagueCoreRepo: LeagueCoreRepository,
    val leagueConfigRepo: LeagueConfigRepository,
    val leagueMemberRepo: LeagueMemberRepository,
    val draftAdminRepo: DraftAdminRepository,
    val draftExecutionService: DraftExecutionService
) {

    suspend fun execute(
        input: DraftInput, type: DraftMessageType, tcId: Long, userId: Long, roleIds: List<Long>
    ): CalcResult<DraftExecution> = tx {
        val leagueData = leagueCoreRepo.getDraftRelevantData(tcId) ?: return@tx K18n_League.NoDraftInChannel.error()
        val pickContext =
            leagueData.checkPickPermission(userId, roleIds).getOrReturn<PickContext, DraftExecution> { return@tx it }
        val config = leagueConfigRepo.getConfig(leagueData.leagueName)
        draftExecutionService.processDraftInput(
            DraftRunContext(leagueData, config, pickContext.currentIdx(leagueData)),
            input,
            type,
            pickContext
        )
    }

    private suspend fun DraftRelevantLeagueData.checkPickPermission(
        uid: Long, roleIds: List<Long>
    ): CalcResult<PickContext> {
        val idxOfParticipant = leagueMemberRepo.getIdxOfParticipant(leagueName, uid)
        if (pseudoEnd && afterTimerSkipMode == AfterTimerSkipMode.AfterDraftUnordered) {
            if (idxOfParticipant == null) {
                leagueMemberRepo.getSingleParticipantAsSubstitute(leagueName, uid)?.takeIf { hasMovedTurns(it) }?.let {
                    return PickContext.AfterDraftUnordered(it).success<PickContext>()
                }
                return K18n_League.NoOpenPicks.error<PickContext>()
            }
            if (hasMovedTurns(idxOfParticipant)) return PickContext.AfterDraftUnordered(idxOfParticipant).success<PickContext>()
            return K18n_League.NoOpenPicks.error<PickContext>()
        }
        if (potentialBetweenPick && idxOfParticipant != null) {
            if (hasMovedTurns(idxOfParticipant)) return PickContext.InBetweenPick(idxOfParticipant).success<PickContext>()
        }
        if (leagueMemberRepo.isAuthorizedFor(leagueName, currentIdx, uid)) return PickContext.RegularTurn.success<PickContext>()
        if (draftAdminRepo.isAdmin(guild, uid, roleIds)) return PickContext.RegularTurn.success<PickContext>()
        return K18n_League.NotYourTurn.error<PickContext>()
    }
}

data class DraftExecution(val results: List<DraftActionResult>, val snipeMap: Map<Int, MutableList<SnipeData>>)
data class SnipeData(val pokemon: String, val sniper: Int)

class DraftExecutionService(
    val picksRepo: LeaguePickRepository,
    val validationService: DraftValidationService,
    val sheetTemplateRepo: SheetTemplateRepository,
    val timerSkipModeDispatcher: TimerSkipModeDispatcher,
    val queuedPicksRepo: QueuedPicksRepository
) {
    suspend fun processDraftInput(
        ctx: DraftRunContext, originalInput: DraftInput, originalType: DraftMessageType, pickContext: PickContext
    ): CalcResult<DraftExecution> {
        val league = ctx.league
        val alreadyPicked = picksRepo.getAllPickedIds(league.leagueName)
        val isOneTimerForAllPicks = ctx.config.timer?.oneTimerForAllPicks == true
        val allResults = mutableListOf<DraftActionResult>()
        val draftData = league.draftData
        val allQueuedPicks = queuedPicksRepo.getForLeague(league.leagueName)
        val snipeMap = mutableMapOf<Int, MutableList<SnipeData>>()
        val modifiedQueue = mutableSetOf<Int>()
        var draftInput = originalInput
        var draftMessageType = originalType
        outer@ while (true) {
            val validatedData = validationService.validateDraftInput(ctx, draftInput, draftMessageType, alreadyPicked)
            if (validatedData.isError()) {
                if (draftMessageType === DraftMessageType.QUEUE) break
                return CalcResult.Error(validatedData.message)
            }
            val actionResult = handleSingleInput(ctx, draftInput, validatedData.value, draftMessageType)
            val nextPlayerData: NextPlayerData =
                if (pickContext is PickContext.InBetweenPick) NextPlayerData.InBetween else NextPlayerData.Normal
            allResults += actionResult
            alreadyPicked.adjustPicks(originalInput)
            checkForSnipes(ctx.activeIdx, allQueuedPicks, snipeMap, modifiedQueue, originalInput.pokemon.showdownId)
            val timerSkipData = timerSkipModeDispatcher.afterPick(ctx, nextPlayerData)
            timerSkipData.message?.let { actionResult.sendsMessage += it }
            var currentIdx = league.currentIdx
            if (timerSkipData.result == TimerSkipResult.NEXT) {
                draftData.timer.lastStallSecondUsedMid?.takeIf { it > 0 }?.let {
                    actionResult.editsMessage += it to { helper ->
                        if (nextPlayerData.isNormalPick()) {
                            val timeStr = TimeUtils.secondsToTimePretty(
                                (draftData.timer.cooldown - System.currentTimeMillis()) / 1000
                            )
                            val currentMention = helper.getPingForUser(league.currentIdx)
                            b {
                                K18n_League.StallSecondsRemaining(
                                    currentMention, timeStr()
                                )()
                            }
                        } else K18n_League.StallSecondsUsedUp(helper.getPingForUser(league.currentIdx))
                    }
                }
                league.nextUser()
                val draftEnded = league.checkFinish()
                if (draftEnded != null) {
                    allResults += draftEnded
                    break
                }
                if (isOneTimerForAllPicks && nextPlayerData.isMoved()) {
                    while (league.currentIdx == currentIdx) {
                        league.addToMoved(currentIdx)
                        currentIdx = league.currentIdx
                        league.nextUser()
                        val draftEnded = league.checkFinish()
                        if (draftEnded != null) {
                            allResults += draftEnded
                            break@outer
                        }
                    }
                }
            }
            if (nextPlayerData is NextPlayerData.InBetween) break
            val nextIdx = if (timerSkipData.result == TimerSkipResult.NOCONCRETE) {
                league.nextUser()
                draftData.moved.values.flatten().toSet().shuffled()
                    .firstOrNull { allQueuedPicks[it]?.let { qp -> qp.enabled && qp.queued.isNotEmpty() } == true }
                    ?: break
            } else league.currentIdx
            draftInput = ctx.getQueuedPickForUser(nextIdx, allQueuedPicks) ?: break
            draftMessageType = DraftMessageType.QUEUE
            ctx.activeIdx = nextIdx
        }
        queuedPicksRepo.updateForLeague(league.leagueName, allQueuedPicks.filter { it.key in modifiedQueue })
        return DraftExecution(allResults, snipeMap).success()
    }

    private fun DraftRunContext.getQueuedPickForUser(idx: Int, allQueuedPicks: Map<Int, QueuePicksUserData>): DraftInput? {
        config.draftBan?.banRounds[league.round]?.let { return null }
        config.randomPickRound?.takeIf { league.round in it.rounds }?.let {
            // TODO: RandomPickRound
        }
        return allQueuedPicks[idx]?.takeIf { it.enabled }?.queued?.firstOrNull()?.buildDraftInput()
    }

    private fun checkForSnipes(
        activeIdx: Int,
        allQueuedPicks: Map<Int, QueuePicksUserData>,
        snipeMap: MutableMap<Int, MutableList<SnipeData>>,
        modifiedQueue: MutableSet<Int>,
        pickedMonId: String
    ) {
        allQueuedPicks.entries.filter { it.value.queued.any { mon -> mon.g.id == pickedMonId } }.forEach { (mem, data) ->
            data.queued.removeIf { mon -> mon.g.id == pickedMonId }
            modifiedQueue.add(mem)
            if (mem != activeIdx) {
                snipeMap.getOrPut(mem) { mutableListOf() }.add(SnipeData(pokemon = pickedMonId, sniper = activeIdx))
                data.enabled = data.enabled && !data.disableIfSniped
            }
        }
    }

    private fun DraftRelevantLeagueData.nextUser() {
        if (draftData.round > totalRounds) return
        if (indexInRound == draftOrder[round]!!.lastIndex) {
            draftData.round++
            draftData.indexInRound = 0
        } else {
            draftData.indexInRound++
        }
    }

    private fun DraftRelevantLeagueData.checkFinish(): DraftActionResult.DraftFinished? {
        if (round > totalRounds) {
            return DraftActionResult.DraftFinished(K18n_League.DraftFinished)
        }
        // TODO: /finishdraft DraftFinishedBecauseAllFinished
        return null
    }

    private fun MutableSet<String>.adjustPicks(input: DraftInput) {
        add(input.pokemon.showdownId)
        input.freesPokemon?.let { remove(it) }
    }

    suspend fun handleSingleInput(
        ctx: DraftRunContext, input: DraftInput, validatedData: ValidationSuccess, type: DraftMessageType
    ): DraftActionResult {
        val (leagueData, config, idx) = ctx
        return when (input) {
            is PickInput -> {
                val pickIndex = picksRepo.saveNewPick(
                    ctx.league.leagueName,
                    idx,
                    input.pokemon.showdownId,
                    validatedData.saveTier,
                    validatedData.freePick,
                    input.noCost,
                    input.tera
                )
                val data = PickData(
                    userIndex = idx,
                    pickIndex = pickIndex,
                    tlName = input.pokemon.tlName,
                    showdownId = input.pokemon.showdownId,
                    tier = validatedData.saveTier,
                    roundIndex = ctx.league.round - 1,
                    free = validatedData.freePick,
                    updrafted = validatedData.updrafted,
                    tera = input.tera
                )
                DraftActionResult.UserAction(
                    round = ctx.league.round, idx = ctx.league.currentIdx, type = type, sheetUpdate = {
                        sheetTemplateRepo.getPickTemplate(config.sheetTemplateId)?.let { template ->
                            applySheetTemplate(template, data)
                        }
                    }, input = input
                )
            }

            is SwitchInput -> {
                val index = picksRepo.saveSwitch(
                    ctx.league.leagueName,
                    idx,
                    input.oldmon,
                    input.pokemon.showdownId,
                    validatedData.saveTier,
                    replace = config.triggers.replaceOnSwitch
                )
                val data = SwitchData(
                    userIndex = idx,
                    pickIndex = index,
                    tlName = input.pokemon.tlName,
                    showdownId = input.pokemon.showdownId,
                    tier = validatedData.saveTier,
                    roundIndex = ctx.league.round - 1,
                    oldTlName = input.oldmon, // TODO use actual tl name here with new system
                    oldShowdownId = input.oldmon
                )
                DraftActionResult.UserAction(
                    round = ctx.league.round, idx = ctx.league.currentIdx, type = type, sheetUpdate = {
                        sheetTemplateRepo.getSwitchTemplate(config.sheetTemplateId)?.let { template ->
                            applySheetTemplate(template, data)
                        }
                    }, input = input
                )
            }

            is BanInput -> {
                val data = BanData(
                    userIndex = idx,
                    pickIndex = -1,
                    tlName = input.pokemon.tlName,
                    showdownId = input.pokemon.showdownId,
                    tier = validatedData.saveTier,
                    roundIndex = ctx.league.round - 1
                )
                leagueData.draftData.draftBan.bannedMons.getOrPut(leagueData.round) { mutableSetOf() }.add(
                    DraftPokemon(
                        name = input.pokemon.showdownId,
                        tier = validatedData.saveTier,
                    )
                )
                DraftActionResult.UserAction(
                    round = ctx.league.round, idx = ctx.league.currentIdx, type = type, sheetUpdate = {
                        sheetTemplateRepo.getBanTemplate(config.sheetTemplateId)?.let { template ->
                            applySheetTemplate(template, data)
                        }
                    }, input = input
                )
            }
        }
    }
}


@Serializable
enum class PickVariable {
    USER_INDEX, PICK_INDEX, POKEMON, SHOWDOWN_ID, TIER, ROUND_INDEX, FREE, UPDRAFTED, TERA
}

@Serializable
enum class SwitchVariable {
    USER_INDEX, PICK_INDEX, POKEMON, SHOWDOWN_ID, TIER, ROUND_INDEX, OLD_TL_NAME, OLD_SHOWDOWN_ID
}

abstract class DraftData(
    val userIndex: Int,
    val pickIndex: Int,
    val tlName: String,
    val showdownId: String,
    val tier: String,
    val roundIndex: Int,
) : AstEnvironment {
    override fun <T : Any> resolve(variable: Enum<*>, clazz: KClass<T>): T {
        val result = when (variable.name) {
            "USER_INDEX" -> userIndex
            "PICK_INDEX" -> pickIndex
            "POKEMON" -> tlName
            "SHOWDOWN_ID" -> showdownId
            "TIER" -> tier
            "ROUND_INDEX" -> roundIndex
            else -> resolveSpecific(variable)
        }
        require(clazz.isInstance(result)) { "Resolved value $result is not of the expected type ${clazz.simpleName}" }
        return clazz.cast(result)
    }

    abstract fun resolveSpecific(variable: Enum<*>): Any
}

class PickData(
    userIndex: Int,
    pickIndex: Int,
    tlName: String,
    showdownId: String,
    tier: String,
    roundIndex: Int,
    val free: Boolean,
    val updrafted: Boolean,
    val tera: Boolean,
) : DraftData(userIndex, pickIndex, tlName, showdownId, tier, roundIndex) {
    override fun resolveSpecific(variable: Enum<*>): Any {
        return when (variable.name) {
            "FREE" -> free
            "UPDRAFTED" -> updrafted
            "TERA" -> tera
            else -> throw IllegalArgumentException("Unknown variable: $variable")
        }
    }
}

class SwitchData(
    userIndex: Int,
    pickIndex: Int,
    tlName: String,
    showdownId: String,
    tier: String,
    roundIndex: Int,
    val oldTlName: String,
    val oldShowdownId: String
) : DraftData(userIndex, pickIndex, tlName, showdownId, tier, roundIndex) {
    override fun resolveSpecific(variable: Enum<*>): Any {
        return when (variable.name) {
            "OLD_TL_NAME" -> oldTlName
            "OLD_SHOWDOWN_ID" -> oldShowdownId
            else -> throw IllegalArgumentException("Unknown variable: $variable")
        }
    }
}

class BanData(
    userIndex: Int, pickIndex: Int, tlName: String, showdownId: String, tier: String, roundIndex: Int
) : DraftData(userIndex, pickIndex, tlName, showdownId, tier, roundIndex) {
    override fun resolveSpecific(variable: Enum<*>): Any {
        throw IllegalArgumentException("No specific variables for BanData")
    }
}

class DraftValidationService(
    val leagueConfigRepo: LeagueConfigRepository,
    val picksRepo: LeaguePickRepository,
    val tierlistRepo: TierlistRepository,
    val tierlistService: TierlistService,
    val priceConfigDispatcher: TierlistPriceConfigDispatcher,
    val banRoundConfigDispatcher: BanRoundConfigDispatcher
) {
    suspend fun validateDraftInput(
        ctx: DraftRunContext, input: DraftInput, type: DraftMessageType, alreadyPicked: Set<String>
    ): CalcResult<ValidationSuccess> {
        if (input.pokemon.showdownId in alreadyPicked) return K18n_DraftUtils.PokemonAlreadyPicked.error()
        val leagueName = ctx.league.leagueName
        val picks = picksRepo.getPicksForUser(leagueName, ctx.activeIdx)
        return when (input) {
            is PickInput -> validatePickInput(ctx, input, type, picks)
            is SwitchInput -> validateSwitchInput(ctx, input, type, picks)
            is BanInput -> validateBanInput(ctx, input, type, picks)
        }
    }

    private suspend fun validatePickInput(
        ctx: DraftRunContext, input: PickInput, type: DraftMessageType, picks: List<DraftPokemon>
    ): CalcResult<ValidationSuccess> {
        val (leagueData, config, idx) = ctx
        if (type != DraftMessageType.RANDOM && config.randomPick.hasJokers()) return K18n_League.LegalRandomPickObligatory.error()
        config.draftBan?.let {
            it.banRounds[leagueData.round]?.let {
                return K18n_League.LegalActionIsNotBan(leagueData.round).error()
            }
        }
        if (picks.count { !it.quit } >= leagueData.teamSize) return K18n_League.TeamFull.error()
        if (leagueData.isSwitchDraft && !config.triggers.allowPickDuringSwitch) return K18n_DraftUtils.NoPickDuringSwitch.error()
        val teraConfig = config.teraPick
        val isTeraPick = input.tera && teraConfig != null
        if (isTeraPick && picks.count { it.tera } >= teraConfig.amount) {
            return K18n_DraftUtils.TeraUserAlreadyPicked.error()
        }
        val tl =
            tierlistRepo.getMeta(leagueData.guild, if (isTeraPick) teraConfig.tlIdentifier else config.tlIdentifier)
                ?: return K18n_NoTierlist.error()
        val official = input.pokemon.showdownId
        val tierData = tierlistService.getTierData(tl, official, input.tier).getOrReturn { return it }
        val context = DraftActionContext()
        with(ValidationRelevantData(picks = picks, idx = idx, teamSize = leagueData.teamSize)) {
            priceConfigDispatcher.handleDraftActionWithGeneralChecks(
                tl.priceManager, DraftAction(
                    tier = tierData, official = official, free = input.free, tera = input.tera, switch = null
                ), context
            )
        }?.let { return it.error() }
        val saveTier = context.saveTier ?: tierData.specified
        return ValidationSuccess(
            saveTier = saveTier, freePick = context.freePick, updrafted = saveTier != tierData.official
        ).success()
    }

    private suspend fun validateSwitchInput(
        ctx: DraftRunContext, input: SwitchInput, type: DraftMessageType, picks: List<DraftPokemon>
    ): CalcResult<ValidationSuccess> {
        val (leagueData, config, idx) = ctx
        config.draftBan?.let {
            it.banRounds[leagueData.round]?.let {
                return K18n_League.LegalActionIsNotBan(leagueData.round).error()
            }
        }
        if (!leagueData.isSwitchDraft) return K18n_DraftUtils.NoSwitchAvailable.error()
        val oldDraftMon =
            picks.firstOrNull { it.name == input.oldmon } ?: return K18n_DraftUtils.PokemonNotInYourTeam(
                input.oldmon // TODO tl name here
            ).error()
        val tl = tierlistRepo.getMeta(leagueData.guild, config.tlIdentifier) ?: return K18n_NoTierlist.error()
        val official = input.pokemon.showdownId
        val tierData = tierlistService.getTierData(tl, official, null).getOrReturn { return it }
        with(ValidationRelevantData(picks = picks, idx = idx, teamSize = leagueData.teamSize)) {
            priceConfigDispatcher.handleDraftActionWithGeneralChecks(
                tl.priceManager, DraftAction(
                    tier = tierData, official = official, free = false, tera = false, switch = oldDraftMon
                ), null
            )?.let { return it.error() }
        }
        return ValidationSuccess(saveTier = tierData.specified, freePick = false, updrafted = false).success()
    }

    private suspend fun validateBanInput(
        ctx: DraftRunContext, input: BanInput, type: DraftMessageType, picks: List<DraftPokemon>
    ): CalcResult<ValidationSuccess> {
        val (leagueData, config, _) = ctx
        val draftBanConfig = config.draftBan ?: return K18n_DraftUtils.BanNotEnabled.error()
        val pokemon = input.pokemon
        val official = pokemon.showdownId
        if (official in draftBanConfig.notBannable) return K18n_DraftUtils.BanNotPossibleForMon(pokemon.tlName).error()

        val banRoundConfig =
            draftBanConfig.banRounds[leagueData.round] ?: return K18n_DraftUtils.NoBanRound(leagueData.round).error()
        val tl = tierlistRepo.getMeta(leagueData.guild, config.tlIdentifier) ?: return K18n_NoTierlist.error()
        val tier = tierlistService.getTierData(tl, official, requestedTier = null)
            .getOrReturn<TierData, ValidationSuccess> { return it }.official
        banRoundConfigDispatcher.checkBan(banRoundConfig, tier, leagueData.alreadyBannedMonsThisRound)?.let { reason ->
            return reason.error()
        }
        return ValidationSuccess(saveTier = tier, freePick = false, updrafted = false).success()
    }
}

data class ValidationSuccess(val saveTier: String, val freePick: Boolean, val updrafted: Boolean)


sealed interface PickContext {

    fun currentIdx(data: DraftRelevantLeagueData): Int

    data object RegularTurn : PickContext {
        override fun currentIdx(data: DraftRelevantLeagueData) = data.currentIdx
    }

    data class AfterDraftUnordered(val idx: Int) : PickContext {
        override fun currentIdx(data: DraftRelevantLeagueData) = idx
    }

    data class InBetweenPick(val idx: Int) : PickContext {
        override fun currentIdx(data: DraftRelevantLeagueData) = idx
    }

}

interface BanRoundConfigOperations<C : BanRoundConfig> {
    fun checkBan(config: C, tier: String, alreadyBanned: Set<DraftPokemon>): K18nMessage?
    fun getPossibleBanTiers(config: C, alreadyBanned: Set<DraftPokemon>): List<String>
}

interface BanRoundConfigHandler<C : BanRoundConfig> : BaseHandler<C>, BanRoundConfigOperations<C>

@Single
class BanRoundConfigDispatcher(handlers: List<BanRoundConfigHandler<BanRoundConfig>>) :
    BanRoundConfigOperations<BanRoundConfig> {
    private val registry = HandlerRegistry(handlers)

    override fun checkBan(
        config: BanRoundConfig, tier: String, alreadyBanned: Set<DraftPokemon>
    ) = registry.getHandler(config).checkBan(config, tier, alreadyBanned)

    override fun getPossibleBanTiers(
        config: BanRoundConfig, alreadyBanned: Set<DraftPokemon>
    ) = registry.getHandler(config).getPossibleBanTiers(config, alreadyBanned)
}

@Single
class FixedTierBanRoundConfigHandler : BanRoundConfigHandler<BanRoundConfig.FixedTier> {
    override val targetClass = BanRoundConfig.FixedTier::class

    override fun checkBan(
        config: BanRoundConfig.FixedTier, tier: String, alreadyBanned: Set<DraftPokemon>
    ) = if (config.tier != tier) K18n_BanRoundConfig.FixedTierError(config.tier) else null

    override fun getPossibleBanTiers(
        config: BanRoundConfig.FixedTier, alreadyBanned: Set<DraftPokemon>
    ) = listOf(config.tier)
}

@Single
class FixedTierSetBanRoundConfigHandler : BanRoundConfigHandler<BanRoundConfig.FixedTierSet> {
    override val targetClass = BanRoundConfig.FixedTierSet::class

    override fun checkBan(
        config: BanRoundConfig.FixedTierSet, tier: String, alreadyBanned: Set<DraftPokemon>
    ): K18nMessage? {
        val originalBanAmount = config.tierSet[tier] ?: return K18n_BanRoundConfig.FixedTierSetTierNotBannable(tier)
        val alreadyBannedAmount = alreadyBanned.count { it.tier == tier }
        return if (originalBanAmount - alreadyBannedAmount <= 0) K18n_BanRoundConfig.FixedTierSetCantBanFromThatTier(
            tier
        ) else null
    }

    override fun getPossibleBanTiers(
        config: BanRoundConfig.FixedTierSet, alreadyBanned: Set<DraftPokemon>
    ): List<String> {
        val alreadyBanned = alreadyBanned.groupBy { it.tier }
        return config.tierSet.entries.filter { it.value - (alreadyBanned[it.key]?.size ?: 0) > 0 }.map { it.key }
    }
}

@Serializable
sealed interface BanRoundConfig {

    @Serializable
    @SerialName("FixedTier")
    data class FixedTier(val tier: String) : BanRoundConfig

    @Serializable
    @SerialName("FixedTierSet")
    data class FixedTierSet(val tierSet: Map<String, Int>) : BanRoundConfig
}


enum class BanSkipBehavior {
    NOTHING, RANDOM
}


sealed interface NextPlayerData {
    data object Normal : NextPlayerData
    data object InBetween : NextPlayerData
    data class Moved(val reason: SkipReason, val skippedUser: Int, val skippedBy: Long? = null) : NextPlayerData

}

@OptIn(ExperimentalContracts::class)
fun NextPlayerData.isNormalPick(): Boolean {
    contract {
        returns(true) implies (this@isNormalPick is NextPlayerData.Normal)
    }
    return this is NextPlayerData.Normal
}

@OptIn(ExperimentalContracts::class)
fun NextPlayerData.isMoved(): Boolean {
    contract {
        returns(true) implies (this@isMoved is NextPlayerData.Moved)
    }
    return this is NextPlayerData.Moved
}

enum class SkipReason {
    REALTIMER, SKIP
}

interface DisplayHelper {
    suspend fun buildAnnounceData(idx: Int): K18nMessage?
    suspend fun getPingForUser(idx: Int): String
}

data class TimerSkipData(
    val result: TimerSkipResult,
    val message: (suspend (DisplayHelper) -> K18nMessage)? = null,
    val cancelTimer: Boolean = false
)

enum class TimerSkipResult {
    NEXT, SAME, NOCONCRETE
}

fun TimerSkipResult.defaultData() = TimerSkipData(this)

sealed class DraftActionResult {
    open var sheetUpdate: (suspend SheetUpdateContext.() -> Unit)? = null
    val editsMessage: MutableList<Pair<Long, suspend (DisplayHelper) -> K18nMessage>> = mutableListOf()
    val sendsMessage: MutableList<suspend (DisplayHelper) -> K18nMessage> = mutableListOf()


    data class UserAction(
        val round: Int,
        val idx: Int,
        val type: DraftMessageType,
        val input: DraftInput,
        override var sheetUpdate: (suspend SheetUpdateContext.() -> Unit)? = null
    ) : DraftActionResult()

    data class TimerSkip(
        val round: Int, val idx: Int
    ) : DraftActionResult()

    data class DraftFinished(val msg: K18nMessage) : DraftActionResult() {
        init {
            sendsMessage += { msg }
        }
    }
}
