package de.tectoast.emolga.domain.league.gamedata.service

import de.tectoast.emolga.discord.DMSender
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.gamedata.repository.GameDataRepository
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.league.schedule.repository.LeagueScheduleRepository
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.BotConstants
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.joinToTeammates
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import org.koin.core.annotation.Single

@Single
class GameReminderService(
    private val replayDataRepo: GameDataRepository,
    private val leagueScheduleRepo: LeagueScheduleRepository,
    private val leagueMemberRepo: LeagueMemberRepository,
    private val dmSender: DMSender,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val languageRepo: GuildConfigRepository,
    private val botConstants: BotConstants
) {
    suspend fun sendReminder(leagueName: String, week: Int, battleIndex: Int) {
        if (replayDataRepo.getFullGameData(leagueName, week, battleIndex) != null) return
        val toRemind = leagueScheduleRepo.getMatchUp(leagueName, week, battleIndex) ?: return
        val primaryIds = leagueMemberRepo.getPrimaryIds(leagueName, toRemind)
        val guild = leagueCoreRepo.getScalarLeagueDataOrNull(leagueName)?.guild ?: return
        val language = languageRepo.getLanguage(guild)
        for ((index, idx) in toRemind.withIndex()) {
            val opponent = toRemind[1 - index]
            primaryIds[idx].orEmpty().forEach { uid ->
                dmSender.sendDM(
                    uid,
                    MessageCreate(
                        embeds = Embed(
                            title = "Reminder", description = K18n_League.ReminderToParticipant(
                                week,
                                primaryIds[opponent].orEmpty().joinToTeammates().ifEmpty { "N/A" },
                                botConstants.botOwnerTag
                            ).translateTo(language), color = Constants.EMBED_COLOR
                        ).into()
                    )
                )
            }
        }
    }
}
