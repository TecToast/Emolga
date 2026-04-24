package de.tectoast.emolga.database.league

import de.tectoast.emolga.database.exposed.DraftLogRepository
import de.tectoast.emolga.database.exposed.GuildLanguageRepository
import de.tectoast.emolga.utils.SpreadsheetService
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.draft.K18n_DraftUtils
import de.tectoast.emolga.utils.invoke
import de.tectoast.emolga.utils.k18n
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.messages.invoke
import kotlinx.serialization.Serializable

class DraftExecutionHandler(
    val languageRepo: GuildLanguageRepository,
    val snipeNotificationService: SnipeNotificationService,
    val draftLogRepository: DraftLogRepository,
    val spreadsheetService: SpreadsheetService,
    val displayService: DraftDisplayService
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
        snipeNotificationService.handleDraftSnipes(leagueName, league.displayName, execution.snipeMap, language)
        val modifiedRounds = mutableSetOf<Int>()
        val preparedDraftLogEntries = execution.results.mapNotNull {
            val round = it.round
            val idx = it.idx
            val logEntry = when (it) {
                is DraftActionResult.UserAction -> it.toLogEntry()
                is DraftActionResult.Skip -> it.toLogEntry()
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
    DraftLogEntry.Action(input = input, type = type, forRound = forRound)

private fun DraftActionResult.Skip.toLogEntry() =
    DraftLogEntry.Skip(reason = reason, madeUpRound = null, skippedBy = skippedBy)

@Serializable
sealed interface DraftLogEntry {

    fun toMessageContent(userRef: String): K18nMessage

    @Serializable
    data class Action(val input: DraftInput, val type: DraftMessageType, val forRound: Int?, val byUser: Long?) :
        DraftLogEntry {
        override fun toMessageContent(userRef: String): K18nMessage {
            val actionText = when (input) {
                is PickInput -> input.pokemon.tlName
                is SwitchInput -> "${input.oldmon} -> ${input.pokemon.tlName}"
                is BanInput -> "BAN ${input.pokemon.tlName}"
            }
            val base = "$actionText ($userRef)"
            if (forRound == null) return base.k18n
            return b {
                "$base [${K18n_DraftUtils.PickedForRound(forRound)()}]"
            }
        }
    }

    @Serializable
    data class Skip(val madeUpRound: Int?, val reason: SkipReason, val skippedBy: Long?) : DraftLogEntry {
        override fun toMessageContent(userRef: String): K18nMessage {
            val base = "N/A" + (skippedBy?.let { " <- <@$skippedBy>" } ?: "") + " ($userRef)"
            if (madeUpRound == null) return base.k18n
            return b {
                "$base [${K18n_DraftUtils.MadeUpInRound(madeUpRound)()}]"
            }
        }
    }

}

data class PreparedDraftLogEntry(val round: Int, val idx: Int, val entry: DraftLogEntry)
