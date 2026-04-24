package de.tectoast.emolga.database.league

import de.tectoast.emolga.database.coord.AstEnvironment
import de.tectoast.emolga.database.exposed.QueuePicksUserData
import de.tectoast.emolga.database.exposed.QueuedPicksRepository
import de.tectoast.emolga.database.exposed.SheetTemplateRepository
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.CalcResult
import de.tectoast.emolga.utils.json.isError
import de.tectoast.emolga.utils.json.success
import de.tectoast.k18n.generated.K18nMessage
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.cast

data class DraftExecution(
    val results: List<DraftActionResult>,
    val snipeMap: Map<Int, SnipeMeta>,
    val timerOption: TimerOption,
    val idxToAnnounce: Int?
)

data class SnipeMeta(val disableIfSniped: Boolean, val list: MutableList<SnipeData>)
data class SnipeData(val pokemon: String, val sniper: Int)

private data class DraftExecutionState(
    val alreadyPicked: MutableSet<String>,
    val allQueuedPicks: Map<Int, QueuePicksUserData>,
    val snipeMap: MutableMap<Int, SnipeMeta>,
    val modifiedQueue: MutableSet<Int>,
    val allResults: MutableList<DraftActionResult>,
    val isOneTimerForAllPicks: Boolean
)

class DraftExecutionService(
    val picksRepo: LeaguePickRepository,
    val validationService: DraftValidationService,
    val sheetTemplateRepo: SheetTemplateRepository,
    val timerSkipModeDispatcher: TimerSkipModeDispatcher,
    val queuedPicksRepo: QueuedPicksRepository
) {
    suspend fun processDraftInput(
        ctx: DraftRunContext,
        originalInput: DraftInput,
        originalType: DraftMessageType,
        pickContext: PickContext,
        byUser: Long?
    ): CalcResult<DraftExecution> {
        val league = ctx.league
        val state = buildDraftExecutionState(ctx)
        var draftInput = originalInput
        var draftMessageType = originalType
        var timerOption = TimerOption.RESTART
        outer@ while (true) {
            val validatedData =
                validationService.validateDraftInput(ctx, draftInput, draftMessageType, state.alreadyPicked)
            if (validatedData.isError()) {
                if (draftMessageType === DraftMessageType.QUEUE) break
                return CalcResult.Error(validatedData.message)
            }
            val actionResult = handleSingleInput(ctx, draftInput, validatedData.value, draftMessageType, byUser.takeUnless { draftMessageType === DraftMessageType.QUEUE })
            val nextPlayerData: NextPlayerData =
                if (pickContext is PickContext.InBetweenPick) NextPlayerData.InBetween else NextPlayerData.Normal
            state.allResults += actionResult
            val afterActionResult = continueAfterAction(ctx, state, actionResult, nextPlayerData, draftInput)
            timerOption = afterActionResult.timerOption
            draftInput = afterActionResult.nextInput ?: break
            draftMessageType = DraftMessageType.QUEUE
        }
        queuedPicksRepo.updateForLeague(
            league.leagueName,
            state.allQueuedPicks.filter { it.key in state.modifiedQueue })
        return DraftExecution(
            state.allResults,
            state.snipeMap,
            timerOption,
            if (pickContext is PickContext.InBetweenPick && !pickContext.isActualCurrent) null else league.currentIdx
        ).success()
    }

    suspend fun processSkip(ctx: DraftRunContext, reason: SkipReason, skippedBy: Long?): DraftExecution {
        val league = ctx.league
        val state = buildDraftExecutionState(ctx)
        val skipResult = DraftActionResult.Skip(league.round, league.currentIdx, reason, skippedBy)
        state.allResults += skipResult
        var afterActionResult = continueAfterAction(
            ctx,
            state,
            skipResult,
            NextPlayerData.Moved(SkipReason.REALTIMER, league.currentIdx),
            null
        )
        var timerOption = afterActionResult.timerOption
        while (afterActionResult.nextInput != null) {
            val draftInput = afterActionResult.nextInput
            val validatedData =
                validationService.validateDraftInput(ctx, draftInput, DraftMessageType.QUEUE, state.alreadyPicked)
            if (validatedData.isError()) break
            val actionResult = handleSingleInput(ctx, draftInput, validatedData.value, DraftMessageType.QUEUE, byUser = null)
            state.allResults += actionResult
            afterActionResult = continueAfterAction(ctx, state, actionResult, NextPlayerData.Normal, draftInput)
            timerOption = afterActionResult.timerOption
        }
        queuedPicksRepo.updateForLeague(
            league.leagueName,
            state.allQueuedPicks.filter { it.key in state.modifiedQueue })
        return DraftExecution(state.allResults, state.snipeMap, timerOption, league.currentIdx)
    }

    private suspend fun buildDraftExecutionState(
        ctx: DraftRunContext
    ): DraftExecutionState {
        val leagueName = ctx.league.leagueName
        return DraftExecutionState(
            alreadyPicked = picksRepo.getAllPickedIds(leagueName),
            allQueuedPicks = queuedPicksRepo.getForLeague(leagueName),
            snipeMap = mutableMapOf(),
            modifiedQueue = mutableSetOf(),
            allResults = mutableListOf(),
            isOneTimerForAllPicks = ctx.config.timer?.oneTimerForAllPicks == true
        )
    }

    private suspend fun continueAfterAction(
        ctx: DraftRunContext,
        state: DraftExecutionState,
        actionResult: DraftActionResult,
        nextPlayerData: NextPlayerData,
        processedInput: DraftInput?
    ): AfterActionResult {
        val league = ctx.league
        val draftData = league.draftData
        processedInput?.let {
            state.alreadyPicked.adjustPicks(it)
            checkForSnipes(
                ctx.activeIdx,
                state.allQueuedPicks,
                state.snipeMap,
                state.modifiedQueue,
                it.pokemon.showdownId
            )
        }
        val timerSkipData = timerSkipModeDispatcher.afterPick(ctx, nextPlayerData)
        timerSkipData.message?.let { actionResult.sendsMessage += it }
        val defaultTimerOption = if (timerSkipData.cancelTimer) TimerOption.CANCEL else TimerOption.RESTART
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
                                currentMention.content, timeStr()
                            )()
                        }
                    } else K18n_League.StallSecondsUsedUp(helper.getPingForUser(league.currentIdx).content)
                }
            }
            league.nextUser()
            val draftEnded = league.checkFinish()
            if (draftEnded != null) {
                state.allResults += draftEnded
                return AfterActionResult(timerOption = TimerOption.CANCEL)
            }
            if (state.isOneTimerForAllPicks && nextPlayerData.isMoved()) {
                while (league.currentIdx == currentIdx) {
                    league.addToMoved(currentIdx)
                    currentIdx = league.currentIdx
                    league.nextUser()
                    val draftEnded = league.checkFinish()
                    if (draftEnded != null) {
                        state.allResults += draftEnded
                        return AfterActionResult(timerOption = TimerOption.CANCEL)
                    }
                }
            }
        }
        if (nextPlayerData is NextPlayerData.InBetween) return AfterActionResult(timerOption = TimerOption.KEEP)
        val nextIdx = if (timerSkipData.result == TimerSkipResult.NOCONCRETE) {
            league.nextUser()
            draftData.moved.values.flatten().toSet().shuffled()
                .firstOrNull { state.allQueuedPicks[it]?.let { qp -> qp.enabled && qp.queued.isNotEmpty() } == true }
                ?: return AfterActionResult(timerOption = defaultTimerOption)
        } else league.currentIdx
        val nextQueuedPick = ctx.getQueuedPickForUser(nextIdx, state.allQueuedPicks) ?: return AfterActionResult(
            timerOption = defaultTimerOption
        )
        ctx.activeIdx = nextIdx
        return AfterActionResult(nextInput = nextQueuedPick, timerOption = defaultTimerOption)
    }

    private fun DraftRunContext.getQueuedPickForUser(
        idx: Int,
        allQueuedPicks: Map<Int, QueuePicksUserData>
    ): DraftInput? {
        config.draftBan?.banRounds[league.round]?.let { return null }
        config.randomPickRound?.takeIf { league.round in it.rounds }?.let {
            // TODO: RandomPickRound
        }
        return allQueuedPicks[idx]?.takeIf { it.enabled }?.queued?.firstOrNull()?.buildDraftInput()
    }

    private fun checkForSnipes(
        activeIdx: Int,
        allQueuedPicks: Map<Int, QueuePicksUserData>,
        snipeMap: MutableMap<Int, SnipeMeta>,
        modifiedQueue: MutableSet<Int>,
        pickedMonId: String
    ) {
        allQueuedPicks.entries.filter { it.value.queued.any { mon -> mon.g.id == pickedMonId } }
            .forEach { (mem, data) ->
                data.queued.removeIf { mon -> mon.g.id == pickedMonId }
                modifiedQueue.add(mem)
                if (mem != activeIdx) {
                    snipeMap.getOrPut(mem) {
                        SnipeMeta(
                            disableIfSniped = data.disableIfSniped,
                            list = mutableListOf()
                        )
                    }.list.add(SnipeData(pokemon = pickedMonId, sniper = activeIdx))
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
        ctx: DraftRunContext, input: DraftInput, validatedData: ValidationSuccess, type: DraftMessageType, byUser: Long?
    ): DraftActionResult {
        val (leagueData, config, _, idx) = ctx
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
                    round = ctx.league.round,
                    forRound = timerSkipModeDispatcher.getPickRound(ctx),
                    idx = ctx.league.currentIdx,
                    type = type,
                    sheetUpdate = {
                        sheetTemplateRepo.getPickTemplate(config.sheetTemplateId)?.let { template ->
                            applySheetTemplate(template, data)
                        }
                    },
                    input = input,
                    byUser = byUser
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
                    round = ctx.league.round,
                    forRound = timerSkipModeDispatcher.getPickRound(ctx),
                    idx = ctx.league.currentIdx,
                    type = type,
                    sheetUpdate = {
                        sheetTemplateRepo.getSwitchTemplate(config.sheetTemplateId)?.let { template ->
                            applySheetTemplate(template, data)
                        }
                    },
                    input = input,
                    byUser = byUser
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
                    round = ctx.league.round,
                    forRound = timerSkipModeDispatcher.getPickRound(ctx),
                    idx = ctx.league.currentIdx,
                    type = type,
                    sheetUpdate = {
                        sheetTemplateRepo.getBanTemplate(config.sheetTemplateId)?.let { template ->
                            applySheetTemplate(template, data)
                        }
                    },
                    input = input,
                    byUser = byUser
                )
            }
        }
    }
}

data class AfterActionResult(val nextInput: DraftInput? = null, val timerOption: TimerOption)

enum class TimerOption {
    RESTART, KEEP, CANCEL
}

sealed class DraftActionResult {
    open var sheetUpdate: (suspend SheetUpdateContext.() -> Unit)? = null
    val editsMessage: MutableList<Pair<Long, suspend (DisplayHelper) -> K18nMessage>> = mutableListOf()
    val sendsMessage: MutableList<suspend (DisplayHelper) -> K18nMessage> = mutableListOf()

    abstract val round: Int
    abstract val idx: Int


    data class UserAction(
        override val round: Int,
        override val idx: Int,
        val forRound: Int,
        val type: DraftMessageType,
        val input: DraftInput,
        val byUser: Long?,
        override var sheetUpdate: (suspend SheetUpdateContext.() -> Unit)? = null
    ) : DraftActionResult()

    data class Skip(
        override val round: Int, override val idx: Int, val reason: SkipReason, val skippedBy: Long?
    ) : DraftActionResult()

    data class DraftFinished(val msg: K18nMessage) : DraftActionResult() {
        override val round = -1
        override val idx = -1

        init {
            sendsMessage += { msg }
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
