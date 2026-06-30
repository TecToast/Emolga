package de.tectoast.emolga.domain.league.draft.service.execution

import de.tectoast.emolga.domain.language.repository.GuildLanguageRepository
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

@Single
class DraftExecutionHandler(
    private val languageRepo: GuildLanguageRepository,
    private val snipeNotificationService: SnipeNotificationService,
    private val draftLogRepository: DraftLogRepository,
    private val spreadsheetService: SpreadsheetService,
    private val displayService: DraftDisplayService
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
        val preparedDraftLogEntries = execution.results.mapNotNull {
            val round = it.round
            val idx = it.idx
            val logEntry = when (it) {
                is DraftActionResult.UserAction -> it.toLogEntry()
                is DraftActionResult.Skip -> it.toLogEntry()
                is DraftActionResult.UserFinished -> DraftLogEntry.UserFinished
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

private fun DraftActionResult.UserAction.toLogEntry() =
    DraftLogEntry.Action(input = input, type = type, forRound = forRound, byUser = byUser, showTier = showTier)

private fun DraftActionResult.Skip.toLogEntry() =
    DraftLogEntry.Skip(reason = reason, madeUpRound = null)


