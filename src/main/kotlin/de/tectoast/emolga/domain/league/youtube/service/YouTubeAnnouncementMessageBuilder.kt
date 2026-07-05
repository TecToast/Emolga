package de.tectoast.emolga.domain.league.youtube.service

import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.league.schedule.repository.LeagueScheduleRepository
import de.tectoast.emolga.domain.league.youtube.model.YouTubeMessageConfig
import de.tectoast.emolga.league.K18n_YouTube
import de.tectoast.emolga.utils.joinToTeammates
import org.koin.core.annotation.Single

@Single
class YouTubeAnnouncementMessageBuilder(
    private val leagueScheduleRepo: LeagueScheduleRepository,
    private val leagueMemberRepo: LeagueMemberRepository,
    private val languageRepo: GuildConfigRepository,
    private val leagueCoreRepo: LeagueCoreRepository
) {
    suspend fun buildMessage(
        leagueName: String,
        week: Int,
        battleIndex: Int,
        messageConfig: YouTubeMessageConfig,
        videoIds: Map<Int, String>
    ): String? {
        val muData = leagueScheduleRepo.getMatchUp(leagueName, week, battleIndex) ?: return null
        val primaryIds = leagueMemberRepo.getPrimaryIds(leagueName, muData)
        return buildString {
            messageConfig.includeRolePing?.let { append("<@&$it>\n") }
            val language = languageRepo.getLanguage(leagueCoreRepo.getScalarLeagueData(leagueName).guild)
            append(K18n_YouTube.WeekAndBattle(week, battleIndex + 1).translateTo(language))
            append("\n\n")
            append(muData.joinToString(" vs. ") { primaryIds[it]?.joinToTeammates() ?: "Unknown" })
            append("\n\n")
            muData.forEach { idx ->
                val mention = primaryIds[idx]?.joinToTeammates() ?: "Unknown"
                val vid = videoIds[idx]
                append(
                    K18n_YouTube.ViewOfPlayer(
                        mention,
                        vid?.let { "https://youtu.be/$it" } ?: K18n_YouTube.NotUploaded.translateTo(
                            language
                        ))
                        .translateTo(language))
                append("\n")
            }
        }
    }
}
