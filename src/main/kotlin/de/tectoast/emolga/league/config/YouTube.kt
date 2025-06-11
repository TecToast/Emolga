package de.tectoast.emolga.league.config

import de.tectoast.emolga.bot.jda
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
    context(League)
    suspend fun formatMessage(gameday: Int, battle: Int, strategy: VideoProvideStrategy): String {
        return buildString {
            includeRolePing?.let { append("<@&$it>\n") }
            append("**Spieltag $gameday**\n_Kampf ${battle + 1}_\n\n")
            val muData = battleorder[gameday]!![battle]
            append(muData.joinToString(" vs. ") { "<@${table[it]}>" })
            append("\n\n")
            val videoIds = muData.mapIndexed { index, uindex ->
                strategy.run { provideVideoId(index, uindex) }
            }
            val names = jda.getGuildById(guild)!!.retrieveMembersByIds(muData.map { table[it] }).await()
                .associate { it.idLong to it.user.effectiveName }
            videoIds.forEachIndexed { index, vid ->
                val uid = table[muData[index]]
                append("${names[uid]}'s Sicht: ")
                append(vid?.let { "https://www.youtube.com/watch?v=$it" } ?: "_noch nicht hochgeladen_")
                append("\n")
            }
        }
    }
}