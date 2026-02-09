package de.tectoast.emolga.league.config

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.GuildLanguageDB
import de.tectoast.emolga.league.K18n_YouTube
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.VideoProvideStrategy
import dev.minn.jda.ktx.coroutines.await
import kotlinx.serialization.Serializable

@Serializable
data class YouTubeConfig(
    val sendChannel: Long,
    val messageConfig: YouTubeMessageConfig = YouTubeMessageConfig(),
)

@Serializable
data class YouTubeMessageConfig(
    val includeRolePing: Long? = null,
) {
    context(league: League)
    suspend fun formatMessage(gameday: Int, battle: Int, strategy: VideoProvideStrategy): String {
        return buildString {
            includeRolePing?.let { append("<@&$it>\n") }
            val language = GuildLanguageDB.getLanguage(league.guild)
            append(K18n_YouTube.GamedayAndBattle(gameday, battle + 1).translateTo(language))
            append("\n\n")
            val muData = league.battleorder[gameday]!![battle]
            append(muData.joinToString(" vs. ") { "<@${league[it]}>" })
            append("\n\n")
            val videoIds = muData.mapIndexed { index, uindex ->
                strategy.run { league.provideVideoId(index, uindex) }
            }
            val names = jda.getGuildById(league.guild)!!.retrieveMembersByIds(muData.map { league[it] }).await()
                .associate { it.idLong to it.user.effectiveName }
            videoIds.forEachIndexed { index, vid ->
                val uid = league[muData[index]]
                append(
                    K18n_YouTube.ViewOfPlayer(
                        names[uid] ?: "Unknown",
                        vid?.let { "https://www.youtube.com/watch?v=$it" } ?: K18n_YouTube.NotUploaded.translateTo(
                            language
                        ))
                        .translateTo(language))
                append("\n")
            }
        }
    }
}