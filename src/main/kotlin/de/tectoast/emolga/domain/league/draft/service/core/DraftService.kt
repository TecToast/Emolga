package de.tectoast.emolga.domain.league.draft.service.core

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.di.TransactionRunner
import de.tectoast.emolga.domain.league.core.model.DraftRelevantLeagueData
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.draft.model.core.*
import de.tectoast.emolga.domain.league.draft.model.execution.DraftActionResult
import de.tectoast.emolga.domain.league.draft.model.execution.DraftExecution
import de.tectoast.emolga.domain.league.draft.model.execution.TimerOption
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickAction
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickResult
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickUserInput
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.draft.service.execution.DraftExecutionHandler
import de.tectoast.emolga.domain.league.draft.service.random.RandomPickService
import de.tectoast.emolga.domain.league.draft.service.timer.DraftTimerService
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.features.league.draft.K18n_FinishDraft
import de.tectoast.emolga.features.league.draft.K18n_RandomPick
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.*
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.annotation.Single


@Single
class DraftService(
    private val tx: TransactionRunner,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leaguePicksRepo: LeaguePickRepository,
    private val leagueMemberRepo: LeagueMemberRepository,
    private val draftPermissionService: DraftPermissionService,
    private val draftExecutionService: DraftExecutionService,
    private val draftExecutionHandler: DraftExecutionHandler,
    private val draftTimerService: DraftTimerService,
    private val draftRunContextBuilder: DraftRunContextBuilder,
    private val randomPickService: RandomPickService,
    dispatcher: CoroutineDispatcher
) : StartupTask {
    override val priority = -10
    private val draftTimerScope = createCoroutineScope("DraftServiceTimerWorker", dispatcher)
    private val logger = KotlinLogging.logger {}
    override suspend fun onStartup() {
        launchCollectTask(draftTimerService.expiredTimerEvents) {
            val result = executeTimerSkip(it)
            if (result.isError()) {
                logger.error {
                    "Failed to execute timer skip for league $it: ${result.message.translateTo(K18N_DEFAULT_LANGUAGE)}"
                }
            }
        }
        leagueCoreRepo.getAllRunningDraftLeagueData().forEach { leagueData ->
            tx {
                continueDraft(leagueData)
            }
        }
    }

    private fun launchCollectTask(events: Flow<String>, handler: suspend (String) -> Unit) {
        draftTimerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            events.collect(handler)
        }
    }

    private suspend fun continueDraft(leagueData: DraftRelevantLeagueData) {
        val ctx = draftRunContextBuilder.build(leagueData).getOrReturn<DraftRunContext, Unit> { return }
        draftTimerService.continueTimer(ctx)
    }

    suspend fun startDraft(leagueName: String, tcId: Long, switchDraft: Boolean) = tx {
        leagueCoreRepo.setDraftStartData(leagueName, tcId, switchDraft)
        val leagueData =
            leagueCoreRepo.getDraftRelevantData(tcId) ?: return@tx K18n_League.NoDraftInChannel.error<Unit>()
        leaguePicksRepo.deleteFromLeague(leagueName)
        val draftRunContext =
            draftRunContextBuilder.build(leagueData).getOrReturn<DraftRunContext, Unit> { return@tx it }
        val result =
            draftExecutionService.processDraftStart(draftRunContext).getOrReturn<DraftExecution, Unit> { return@tx it }
        handleDraftExecutionAndSave(result, draftRunContext)
        Unit.success()
    }

    private suspend fun buildContext(
        tcId: Long,
        userId: Long,
        roleIds: Collection<Long>
    ): CalcResult<DraftRunContext> {
        val leagueData =
            leagueCoreRepo.getDraftRelevantData(tcId) ?: return K18n_League.NoDraftInChannel.error()
        val pickContext = draftPermissionService.checkPickPermission(leagueData, userId, roleIds)
            .getOrReturn<PickContext, DraftRunContext> { return it }
        return draftRunContextBuilder.build(
            leagueData,
            pickContext
        )
    }

    suspend fun handleInputRequest(
        input: DraftInput,
        tcId: Long,
        userId: Long,
        roleIds: Collection<Long>,
        validationCompleteCallback: suspend () -> Unit = { }
    ) = tx {
        val ctx = buildContext(tcId, userId, roleIds).getOrReturn<DraftRunContext, Unit> { return@tx it }
        val result = draftExecutionService.processDraftInput(
            ctx, input, DraftActionOrigin.REGULAR, byUser = userId
        ).getOrReturn<DraftExecution, Unit> { return@tx it }
        validationCompleteCallback()
        handleDraftExecutionAndSave(result, ctx)
        Unit.success()
    }

    suspend fun handleRandomPickRequest(
        input: RandomPickUserInput,
        tcId: Long,
        userId: Long,
        roleIds: Collection<Long>,
        validationCompleteCallback: suspend () -> Unit = { }
    ): CalcResult<RandomPickResult> = tx {
        val ctx = buildContext(tcId, userId, roleIds).getOrReturn<DraftRunContext, RandomPickResult> { return@tx it }
        val randomPickResult =
            randomPickService.getRandomPick(input, ctx).getOrReturn<RandomPickResult, RandomPickResult> { return@tx it }
        if (randomPickResult is RandomPickResult.RerollPossible) return@tx randomPickResult.success()
        val result = draftExecutionService.processDraftInput(
            ctx, PickInput(randomPickResult.showdownId, tier = input.tier, free = input.free, tera = input.tera),
            DraftActionOrigin.RANDOM, byUser = userId
        ).getOrReturn<DraftExecution, RandomPickResult> { return@tx it }
        validationCompleteCallback()
        handleDraftExecutionAndSave(result, ctx)
        randomPickResult.success()
    }

    suspend fun handleRandomPickFollowUpRequest(
        action: RandomPickAction,
        tcId: Long,
        userId: Long,
        roleIds: Collection<Long>,
        validationCompleteCallback: suspend () -> Unit = { }
    ): CalcResult<RandomPickResult> = tx {
        val ctx = buildContext(tcId, userId, roleIds).getOrReturn<DraftRunContext, RandomPickResult> { return@tx it }
        val randomPickDraftData = ctx.league.draftData.randomPick
        val (showdownId, tier, free, tera, map, history, disabled) = randomPickDraftData.currentMon
            ?: return@tx K18n_RandomPick.NoPickAvailable.error()
        if (disabled) return@tx K18n_RandomPick.YouMustGamble.error()
        val randomPickResult = if (action == RandomPickAction.REROLL) {
            if ((randomPickDraftData.usedJokers[ctx.activeIdx]
                    ?: 0) >= ctx.config.randomPick.jokers
            ) return@tx K18n_RandomPick.NoJokers.error()
            randomPickDraftData.usedJokers.add(ctx.activeIdx, 1)
            val randomPickResult = randomPickService.getRandomPick(
                RandomPickUserInput(tier, map["type"], free, tera, skipMons = history),
                ctx
            ).getOrReturn<RandomPickResult, RandomPickResult> { return@tx it }
            if (randomPickResult is RandomPickResult.RerollPossible) {
                return@tx randomPickResult.success()
            }
            randomPickResult
        } else {
            RandomPickResult.NoReroll(showdownId, tier)
        }
        val result = draftExecutionService.processDraftInput(
            ctx, PickInput(randomPickResult.showdownId, tier = randomPickResult.tier, free = free, tera = tera),
            when (action) {
                RandomPickAction.ACCEPT -> DraftActionOrigin.ACCEPT
                RandomPickAction.REROLL -> DraftActionOrigin.REROLL
            }, byUser = userId
        ).getOrReturn<DraftExecution, RandomPickResult> { return@tx it }
        validationCompleteCallback()
        handleDraftExecutionAndSave(result, ctx)
        randomPickResult.success()
    }

    suspend fun handleSkipRequest(
        tcId: Long,
        userId: Long,
        roleIds: Collection<Long>,
        validationCompleteCallback: suspend () -> Unit = { }
    ): CalcResult<Unit> = tx {
        val leagueData =
            leagueCoreRepo.getDraftRelevantData(tcId) ?: return@tx K18n_League.NoDraftInChannel.error<Unit>()
        val pickContext = draftPermissionService.checkPickPermission(leagueData, userId, roleIds)
            .getOrReturn<PickContext, Unit> { return@tx it }
        val idx = when (pickContext) {
            is PickContext.AfterDraftUnordered, is PickContext.InBetweenPick -> return@tx K18n_League.NotYourTurn.error<Unit>()
            is PickContext.RegularTurn -> {
                pickContext.idx
            }
        }
        validationCompleteCallback()
        val isPrimaryUser = leagueMemberRepo.isPrimaryUser(leagueData.leagueName, idx, userId)
        executeSkip(leagueData, SkipReason.Skip(skippedByExternal = userId.takeUnless { isPrimaryUser }))
    }


    private suspend fun executeTimerSkip(leagueName: String) = tx {
        val leagueData =
            leagueCoreRepo.getDraftRelevantData(leagueName) ?: return@tx K18n_League.NoDraftInChannel.error<Unit>()
        if (!draftTimerService.isValidTimerExecution(leagueData.draftData.timer.cooldown)) {
            return@tx Unit.success()
        }
        executeSkip(leagueData, SkipReason.RealTimer)
        Unit.success()
    }

    private suspend fun executeSkip(
        leagueData: DraftRelevantLeagueData,
        reason: SkipReason
    ): CalcResult<Unit> = tx {
        val ctx = draftRunContextBuilder.build(leagueData).getOrReturn<DraftRunContext, Unit> { return@tx it }
        val result = draftExecutionService.processSkip(ctx, reason, fromUserFinish = false)
        handleDraftExecutionAndSave(result, ctx)
        Unit.success()
    }

    suspend fun handleFinishUserRequest(
        tcId: Long,
        userId: Long,
        validationCompleteCallback: suspend () -> Unit = { }
    ): CalcResult<Unit> = tx {
        val leagueData =
            leagueCoreRepo.getDraftRelevantData(tcId) ?: return@tx K18n_League.NoDraftInChannel.error<Unit>()
        if (!leagueData.isSwitchDraft) return@tx K18n_FinishDraft.NoSupport.error()
        val idx = leagueMemberRepo.getIdxOfParticipant(leagueData.leagueName, userId)
            ?: return@tx K18n_League.NoDraftForYou.error()
        val finishedDraft = leagueData.draftData.finishedDraft
        if (!finishedDraft.add(idx)) {
            return@tx K18n_League.NoDraftForYou.error()
        }
        validationCompleteCallback()
        val ctx = draftRunContextBuilder.build(leagueData).getOrReturn<DraftRunContext, Unit> { return@tx it }
        val result = if (leagueData.currentIdx == idx) {
            draftExecutionService.processSkip(ctx, SkipReason.Skip(), fromUserFinish = true)
        } else {
            DraftExecution(
                results = listOf(DraftActionResult.UserFinished(leagueData.round, idx)),
                snipeMap = emptyMap(),
                timerOption = TimerOption.KEEP,
                idxToAnnounce = null
            )
        }
        handleDraftExecutionAndSave(result, ctx)
        Unit.success()
    }

    private suspend fun handleDraftExecutionAndSave(result: DraftExecution, ctx: DraftRunContext) {
        when (result.timerOption) {
            TimerOption.RESTART -> draftTimerService.startRegularTimer(ctx)
            TimerOption.CANCEL -> draftTimerService.cancelTimer(ctx.league.leagueName)
            TimerOption.KEEP -> {}
        }
        leagueCoreRepo.updateDraftData(ctx.league.leagueName, ctx.league.draftData)
        draftExecutionHandler.handleDraftExecution(result, ctx)
    }

}
