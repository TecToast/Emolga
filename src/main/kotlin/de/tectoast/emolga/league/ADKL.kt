package de.tectoast.emolga.league

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.records.SorterData
import dev.minn.jda.ktx.coroutines.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("ADKL")
class ADKL : League() {
    override val teamsize = 11

    override val duringTimerSkipMode = NEXT_PICK

    @Transient
    override val docEntry = DocEntry.create(this) {
        monsOrder = { list -> list.sortedBy { it.tier.indexedBy(tierlist.order) }.map { it.name } }
        killProcessor = BasicStatProcessor {
            plindex.CoordXMod("Kader", 2, 'P' - 'B', 5 + gdi, 22, monindex + 11)
        }
        deathProcessor = CombinedStatProcessor {
            plindex.CoordXMod("Kader", 2, 'P' - 'B', 5 + gdi, 22, 22)
        }
        sorterData = SorterData(
            "Tabelle!C5:G12",
            indexer = { it.substringAfter("C").substringBefore(":").toInt() },
            newMethod = false,
            cols = listOf(2, -1, 4)
        )
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addStrikethroughChange(57357925, data.round + 1, data.indexInRound + 5, true)
        addSingle(data.idx.CoordXMod("Kader", 2, 'P' - 'B', 3, 22, 11 + data.changedOnTeamsiteIndex), data.pokemon)
    }

    override suspend fun executeYoutubeSend(
        ytTC: Long,
        gameday: Int,
        battle: Int,
        strategy: VideoProvideStrategy,
        overrideEnabled: Boolean
    ) {
        val ytVideoSaveData = persistentData.replayDataStore.data[gameday]?.get(battle)?.ytVideoSaveData
        if (!overrideEnabled && ytVideoSaveData?.enabled != true) return
        ytVideoSaveData?.enabled = false
        jda.getTextChannelById(ytTC)!!.sendMessage(buildString {
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
        save("YTSubSave")
    }
}
