package de.tectoast.emolga.domain.league.draft.service.core

import de.tectoast.emolga.domain.league.core.model.DraftRelevantLeagueData
import de.tectoast.emolga.domain.league.doc.repository.SheetTemplateRepository
import de.tectoast.emolga.domain.league.draft.model.core.*
import de.tectoast.emolga.domain.league.draft.model.execution.*
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.draft.service.timer.skipmode.TimerSkipModeDispatcher
import de.tectoast.emolga.domain.league.draft.util.getDisplayName
import de.tectoast.emolga.domain.league.queue.model.QueuePicksUserData
import de.tectoast.emolga.domain.league.queue.repository.QueuedPicksRepository
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.domain.util.service.TimeFormatService
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.dsl.AstEnvironment
import de.tectoast.emolga.utils.dsl.ValidVariableProvider
import de.tectoast.emolga.utils.sheetupdate.applySheetTemplate
import org.koin.core.annotation.Single
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.time.Clock


@Single
class DraftExecutionService(
    private val picksRepo: LeaguePickRepository,
    private val validationService: DraftValidationService,
    private val sheetTemplateRepo: SheetTemplateRepository,
    private val timerSkipModeDispatcher: TimerSkipModeDispatcher,
    private val queuedPicksRepo: QueuedPicksRepository,
    private val displayService: PokemonDisplayService,
    private val timeFormatService: TimeFormatService,
    private val clock: Clock
) {
    suspend fun processDraftInput(
        ctx: DraftRunContext,
        originalInput: DraftInput,
        originalType: DraftActionOrigin,
        byUser: Long?
    ): CalcResult<DraftExecution> {
        val state = buildDraftExecutionState(ctx)
        return processDraftInput(ctx, originalInput, originalType, byUser, state)
    }

    private suspend fun processDraftInput(
        ctx: DraftRunContext,
        originalInput: DraftInput,
        originalType: DraftActionOrigin,
        byUser: Long?,
        state: DraftExecutionState
    ): CalcResult<DraftExecution> {
        val league = ctx.league
        var draftInput = originalInput
        var draftMessageType = originalType
        var timerOption = TimerOption.RESTART
        outer@ while (true) {
            val validatedData =
                validationService.validateDraftInput(ctx, draftInput, draftMessageType, state.alreadyPicked)
            if (validatedData.isError()) {
                if (draftMessageType === DraftActionOrigin.QUEUE) break
                return CalcResult.Error(validatedData.message)
            }
            val actionResult = handleSingleInput(
                ctx,
                draftInput,
                validatedData.value,
                draftMessageType,
                byUser.takeUnless { draftMessageType === DraftActionOrigin.QUEUE })
            val nextPlayerData: NextPlayerData =
                if (ctx.pickContext is PickContext.InBetweenPick) NextPlayerData.InBetween else NextPlayerData.Normal
            state.allResults += actionResult
            val afterActionResult = continueAfterAction(ctx, state, actionResult, nextPlayerData, draftInput)
            timerOption = afterActionResult.timerOption
            draftInput = afterActionResult.nextInput ?: break
            draftMessageType = DraftActionOrigin.QUEUE
        }
        queuedPicksRepo.updateForLeague(
            league.leagueName,
            state.allQueuedPicks.filter { it.key in state.modifiedQueue })
        return DraftExecution(
            state.allResults,
            state.snipeMap,
            timerOption,
            if ((ctx.pickContext as? PickContext.InBetweenPick)?.isActualCurrent == false) null else league.currentIdx
        ).success()
    }

    suspend fun processSkip(
        ctx: DraftRunContext,
        reason: SkipReason,
        fromUserFinish: Boolean
    ): DraftExecution {
        val league = ctx.league
        val state = buildDraftExecutionState(ctx)
        val skipResult = if (fromUserFinish) DraftActionResult.UserFinished(
            league.round,
            league.currentIdx
        ) else DraftActionResult.Skip(league.round, league.currentIdx, reason)
        state.allResults += skipResult
        var afterActionResult = continueAfterAction(
            ctx,
            state,
            skipResult,
            NextPlayerData.Moved(reason, league.currentIdx),
            null
        )
        var timerOption = afterActionResult.timerOption
        while (afterActionResult.nextInput != null) {
            val draftInput = afterActionResult.nextInput
            val validatedData =
                validationService.validateDraftInput(ctx, draftInput, DraftActionOrigin.QUEUE, state.alreadyPicked)
            if (validatedData.isError()) break
            val actionResult =
                handleSingleInput(ctx, draftInput, validatedData.value, DraftActionOrigin.QUEUE, byUser = null)
            state.allResults += actionResult
            afterActionResult = continueAfterAction(ctx, state, actionResult, NextPlayerData.Normal, draftInput)
            timerOption = afterActionResult.timerOption
        }
        queuedPicksRepo.updateForLeague(
            league.leagueName,
            state.allQueuedPicks.filter { it.key in state.modifiedQueue })
        return DraftExecution(state.allResults, state.snipeMap, timerOption, league.currentIdx)
    }

    suspend fun processDraftStart(ctx: DraftRunContext): CalcResult<DraftExecution> {
        val state = buildDraftExecutionState(ctx)
        val queuedInput =
            ctx.getQueuedPickForUser(ctx.league.currentIdx, state.allQueuedPicks) ?: return DraftExecution(
                emptyList(), emptyMap(),
                TimerOption.RESTART, ctx.league.currentIdx
            ).success()
        return processDraftInput(
            ctx,
            queuedInput,
            DraftActionOrigin.QUEUE,
            byUser = null,
            state
        )
    }

    private suspend fun buildDraftExecutionState(
        ctx: DraftRunContext
    ): DraftExecutionState {
        val leagueName = ctx.league.leagueName
        return DraftExecutionState(
            alreadyPicked = picksRepo.getAllPickedIds(leagueName)
                .apply { addAll(ctx.league.draftData.draftBan.bannedMons.values.flatten().map { it.showdownId }) },
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
                it.pokemon
            )
        }
        if (nextPlayerData is NextPlayerData.Moved) {
            league.addToMoved(ctx.activeIdx)
            val timerConfig = ctx.config.timer
            if (timerConfig != null) {
                if (clock.now() > timerConfig.startPunishSkipsTime) {
                    val isPunishable = when (val reason = nextPlayerData.reason) {
                        SkipReason.RealTimer -> true
                        is SkipReason.Skip -> reason.skippedByExternal != null
                    }
                    if (isPunishable) {
                        draftData.punishableSkippedTurns.getOrPut(ctx.activeIdx) { mutableSetOf() }.add(draftData.round)
                    }
                }
            }
        }
        val timerSkipData = timerSkipModeDispatcher.afterPick(ctx, nextPlayerData)
        timerSkipData.message?.let { actionResult.sendsMessage += it }
        val defaultTimerOption = if (timerSkipData.cancelTimer) TimerOption.CANCEL else TimerOption.RESTART
        var currentIdx = league.currentIdx
        if (timerSkipData.result == TimerSkipResult.NEXT) {
            draftData.timer.lastStallSecondUsedMid?.takeIf { it > 0 }?.let {
                actionResult.editsMessage += it to { helper ->
                    if (nextPlayerData.isNormalPick()) {
                        val timeStr = timeFormatService.durationToPrettyLong(
                            draftData.timer.cooldown - clock.now()
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
            draftData.timer.lastStallSecondUsedMid = null
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
        ctx.pickContext = PickContext.RegularTurn(nextIdx)
        return AfterActionResult(nextInput = nextQueuedPick, timerOption = defaultTimerOption)
    }

    private fun DraftRunContext.getQueuedPickForUser(
        idx: Int,
        allQueuedPicks: Map<Int, QueuePicksUserData>
    ): DraftInput? {
        config.draftBan?.banRounds[league.round]?.let { return null }
        return allQueuedPicks[idx]?.takeIf { it.enabled }?.queued?.firstOrNull()?.buildDraftInput()
    }

    private fun checkForSnipes(
        activeIdx: Int,
        allQueuedPicks: Map<Int, QueuePicksUserData>,
        snipeMap: MutableMap<Int, SnipeMeta>,
        modifiedQueue: MutableSet<Int>,
        pickedMonId: ShowdownID
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
        while (draftOrder[round]?.getOrNull(draftData.indexInRound)?.let { it in draftData.finishedDraft } == true) {
            if (indexInRound == draftOrder[round]!!.lastIndex) {
                draftData.round++
                draftData.indexInRound = 0
            } else {
                draftData.indexInRound++
            }
        }
    }

    private fun DraftRelevantLeagueData.checkFinish(): DraftActionResult.DraftFinished? {
        if (round > totalRounds) {
            return DraftActionResult.DraftFinished(K18n_League.DraftFinished)
        }
        return null
    }

    private fun MutableSet<ShowdownID>.adjustPicks(input: DraftInput) {
        add(input.pokemon)
        input.freesPokemon?.let { remove(it) }
    }

    private suspend fun handleSingleInput(
        ctx: DraftRunContext,
        input: DraftInput,
        validatedData: ValidationSuccess,
        type: DraftActionOrigin,
        byUser: Long?
    ): DraftActionResult {
        val (leagueData, config, _, _) = ctx
        val idx = ctx.activeIdx
        return when (input) {
            is PickInput -> {
                val pickIndex = picksRepo.saveNewPick(
                    ctx.league.guild,
                    ctx.league.leagueName,
                    idx,
                    input.pokemon,
                    validatedData.saveTier,
                    validatedData.freePick,
                    input.noCost,
                    input.tera
                )
                val data = PickData(
                    userIndex = idx,
                    pickIndex = pickIndex,
                    tlName = displayService.getDisplayName(input.pokemon, ctx),
                    showdownId = input.pokemon,
                    tier = validatedData.saveTier,
                    roundIndex = ctx.league.round - 1,
                    free = validatedData.freePick,
                    updrafted = validatedData.updrafted,
                    tera = input.tera,
                    points = validatedData.points
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
                    byUser = byUser,
                    showTier = validatedData.saveTier.takeIf { config.triggers.alwaysShowTier || validatedData.updrafted }
                )
            }

            is SwitchInput -> {
                val index = picksRepo.saveSwitch(
                    ctx.league.guild,
                    ctx.league.leagueName,
                    idx,
                    input.oldmon,
                    input.pokemon,
                    validatedData.saveTier,
                    replace = config.triggers.replaceOnSwitch
                )
                val data = SwitchData(
                    userIndex = idx,
                    pickIndex = index,
                    tlName = displayService.getDisplayName(input.pokemon, ctx),
                    showdownId = input.pokemon,
                    tier = validatedData.saveTier,
                    roundIndex = ctx.league.round - 1,
                    oldTlName = displayService.getDisplayName(input.oldmon, ctx),
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
                    byUser = byUser,
                    showTier = validatedData.saveTier.takeIf { config.triggers.alwaysShowTier || validatedData.updrafted }
                )
            }

            is BanInput -> {
                val data = BanData(
                    userIndex = idx,
                    pickIndex = -1,
                    tlName = displayService.getDisplayName(input.pokemon, ctx),
                    showdownId = input.pokemon,
                    tier = validatedData.saveTier,
                    roundIndex = ctx.league.round - 1
                )
                leagueData.draftData.draftBan.bannedMons.getOrPut(leagueData.round) { mutableSetOf() }.add(
                    DraftPokemon(
                        showdownId = input.pokemon,
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
                    byUser = byUser,
                    showTier = validatedData.saveTier.takeIf { config.triggers.alwaysShowTier || validatedData.updrafted }
                )
            }
        }
    }
}

private data class DraftExecutionState(
    val alreadyPicked: MutableSet<ShowdownID>,
    val allQueuedPicks: Map<Int, QueuePicksUserData>,
    val snipeMap: MutableMap<Int, SnipeMeta>,
    val modifiedQueue: MutableSet<Int>,
    val allResults: MutableList<DraftActionResult>,
    val isOneTimerForAllPicks: Boolean
)

private data class AfterActionResult(val nextInput: DraftInput? = null, val timerOption: TimerOption)

private abstract class DraftData(
    private val userIndex: Int,
    private val pickIndex: Int,
    private val tlName: String,
    val showdownId: ShowdownID,
    val tier: String,
    private val roundIndex: Int,
) : AstEnvironment {
    override fun <T : Any> resolve(variable: String, clazz: KClass<T>): T {
        val result = when (variable) {
            USER_INDEX -> userIndex
            PICK_INDEX -> pickIndex
            POKEMON -> tlName
            SHOWDOWN_ID -> showdownId
            TIER -> tier
            ROUND_INDEX -> roundIndex
            else -> resolveSpecific(variable)
        }
        require(clazz.isInstance(result)) { "Resolved value $result is not of the expected type ${clazz.simpleName}" }
        return clazz.cast(result)
    }

    abstract fun resolveSpecific(variable: String): Any

    companion object {
        const val USER_INDEX = "USER_INDEX"
        const val PICK_INDEX = "PICK_INDEX"
        const val POKEMON = "POKEMON"
        const val SHOWDOWN_ID = "SHOWDOWN_ID"
        const val TIER = "TIER"
        const val ROUND_INDEX = "ROUND_INDEX"
    }
}

private class PickData(
    userIndex: Int,
    pickIndex: Int,
    tlName: String,
    showdownId: ShowdownID,
    tier: String,
    roundIndex: Int,
    val free: Boolean,
    val updrafted: Boolean,
    val tera: Boolean,
    val points: Int?
) : DraftData(userIndex, pickIndex, tlName, showdownId, tier, roundIndex) {
    override fun resolveSpecific(variable: String): Any {
        return when (variable) {
            "FREE" -> free
            "UPDRAFTED" -> updrafted
            "TERA" -> tera
            "POINTS" -> points ?: 0
            else -> throw IllegalArgumentException("Unknown variable: $variable")
        }
    }

    companion object : ValidVariableProvider {
        override val validVariables = setOf(
            FREE, UPDRAFTED, TERA, POINTS,
            USER_INDEX, PICK_INDEX, POKEMON, SHOWDOWN_ID, TIER, ROUND_INDEX
        )
        const val FREE = "FREE"
        const val UPDRAFTED = "UPDRAFTED"
        const val TERA = "TERA"
        const val POINTS = "POINTS"

    }
}

private class SwitchData(
    userIndex: Int,
    pickIndex: Int,
    tlName: String,
    showdownId: ShowdownID,
    tier: String,
    roundIndex: Int,
    val oldTlName: String,
    val oldShowdownId: ShowdownID
) : DraftData(userIndex, pickIndex, tlName, showdownId, tier, roundIndex) {
    override fun resolveSpecific(variable: String): Any {
        return when (variable) {
            OLD_TL_NAME -> oldTlName
            OLD_SHOWDOWN_ID -> oldShowdownId
            else -> throw IllegalArgumentException("Unknown variable: $variable")
        }
    }

    companion object : ValidVariableProvider {
        override val validVariables = setOf(
            OLD_TL_NAME, OLD_SHOWDOWN_ID,
            USER_INDEX, PICK_INDEX, POKEMON, SHOWDOWN_ID, TIER, ROUND_INDEX
        )

        const val OLD_TL_NAME = "OLD_TL_NAME"
        const val OLD_SHOWDOWN_ID = "OLD_SHOWDOWN_ID"
    }
}

private class BanData(
    userIndex: Int, pickIndex: Int, tlName: String, showdownId: ShowdownID, tier: String, roundIndex: Int
) : DraftData(userIndex, pickIndex, tlName, showdownId, tier, roundIndex) {
    override fun resolveSpecific(variable: String): Any {
        throw IllegalArgumentException("No specific variables for BanData (trying $variable)")
    }

    companion object : ValidVariableProvider {
        override val validVariables = setOf(
            USER_INDEX, PICK_INDEX, POKEMON, SHOWDOWN_ID, TIER, ROUND_INDEX
        )
    }
}
