package de.tectoast.emolga.league

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.CoordXMod
import dev.minn.jda.ktx.coroutines.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("WPP")
class WPP : League() {
    override val teamsize = 11

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(data.roundIndex.minus(1).CoordXMod("Draft", 4, 5, 3, 11, 4 + data.indexInRound), data.pokemon)
    }

    override suspend fun executeYoutubeSend(
        ytTC: Long,
        gameday: Int,
        battle: Int,
        strategy: VideoProvideStrategy,
        overrideEnabled: Boolean
    ) {
        val b = builder()
        val ytVideoSaveData = persistentData.replayDataStore.data[gameday]?.get(battle)?.ytVideoSaveData
        if (!overrideEnabled && ytVideoSaveData?.enabled != true) return logger.info("ExecuteYTSend: Not enabled")
        ytVideoSaveData?.enabled = false
        jda.getTextChannelById(ytTC)!!.sendMessage(buildString {
            append("<@&1343723752495644703>\n")
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
        }).queue()
        b.execute()
        save("YTSubSave")
    }
}