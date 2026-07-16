package de.tectoast.emolga.domain.league.draft.service.execution

import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.execution.DraftActionResult
import de.tectoast.emolga.domain.league.draft.model.execution.DraftExecution
import de.tectoast.emolga.domain.league.draft.model.execution.DraftLogEntry
import de.tectoast.emolga.domain.league.draft.model.execution.PreparedDraftLogEntry
import de.tectoast.emolga.domain.league.draft.repository.DraftLogRepository
import de.tectoast.emolga.domain.league.draft.service.execution.display.DraftDisplayService
import de.tectoast.emolga.domain.league.draft.service.util.SnipeNotificationService
import de.tectoast.emolga.utils.sheetupdate.SpreadsheetService
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Instant

@Single
class DraftExecutionHandler(
    private val languageRepo: GuildConfigRepository,
    private val snipeNotificationService: SnipeNotificationService,
    private val draftLogRepository: DraftLogRepository,
    private val spreadsheetService: SpreadsheetService,
    private val displayService: DraftDisplayService,
    private val clock: Clock,
) {
    suspend fun handleDraftExecution(execution: DraftExecution, ctx: DraftRunContext) {
        val league = ctx.league
        spreadsheetService.updateSheet(league.sheetId, wait = false) {
            execution.results.forEach {
                it.sheetUpdate?.invoke(this)
            }
        }
        val leagueName = league.leagueName
        val language = languageRepo.getLanguage(league.guild)
        val sessionId = league.draftData.draftSessionNum
        snipeNotificationService.handleDraftSnipes(
            leagueName,
            ctx.league.guild,
            league.displayName,
            execution.snipeMap,
            language,
            ctx.tierlistMeta.language
        )
        val modifiedRounds = mutableSetOf<Int>()
        val now = clock.now()
        val preparedDraftLogEntries = execution.results.mapNotNull {
            val round = it.round
            val idx = it.idx
            val logEntry = when (it) {
                is DraftActionResult.UserAction -> it.toLogEntry(now)
                is DraftActionResult.Skip -> it.toLogEntry(now)
                is DraftActionResult.UserFinished -> DraftLogEntry.UserFinished(now)
                else -> return@mapNotNull null
            }
            if (it is DraftActionResult.UserAction && it.forRound != it.round) {
                draftLogRepository.setMadeUpRound(leagueName, sessionId, it.forRound, idx, it.round)
                modifiedRounds.add(it.forRound)
            }
            modifiedRounds.add(round)
            PreparedDraftLogEntry(round, idx, logEntry)
        }
        draftLogRepository.insertLogEntries(leagueName, sessionId, preparedDraftLogEntries)
        displayService.handleDraftExecution(ctx, execution, modifiedRounds, language)
    }

}

private fun DraftActionResult.UserAction.toLogEntry(now: Instant) =
    DraftLogEntry.Action(input = input, origin = origin, forRound = forRound.takeIf { it != round }, byUser = byUser, showTier = showTier, timestamp = now)

private fun DraftActionResult.Skip.toLogEntry(now: Instant) =
    DraftLogEntry.Skip(reason = reason, madeUpRound = null, timestamp = now)


