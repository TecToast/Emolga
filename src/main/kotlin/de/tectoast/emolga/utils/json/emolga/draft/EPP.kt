package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.TipGameMessagesDB
import de.tectoast.emolga.features.ArgBuilder
import de.tectoast.emolga.features.draft.TipGameManager
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.interactions.components.ActionRow
import java.awt.Color

@Serializable
@SerialName("EPP")
class EPP(val spoilerSid: String) : League() {
    override val teamsize = 11
    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED

    /*@Transient
    override var timer: DraftTimer? = SimpleTimer(
        TimerInfo(delaysAfterSkips = mapOf(0 to 4 * 60, 1 to 2 * 60, 2 to 60, 3 to 30, 4 to 15)).apply {
            set(10, 22)
            startPunishSkipsTime = 1734177600000
        }
    )*/
    @Transient
    override val docEntry = DocEntry.create(this) {
        monsOrder = { list -> list.sortedBy { it.tier.indexedBy(tierlist.order) }.map { it.name } }
        spoilerDocSid = spoilerSid
        killProcessor = BasicStatProcessor {
            plindex.CoordXMod("Kader", 2, 'P' - 'B', 5 + gdi, 19, monindex + 9)
        }
    }

    override fun sendRound() {
        tc.sendMessage("## === Runde ${if (round == 1) "Bann" else round - 1} ===").queue()
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(data.roundIndex.minus(1).CoordXMod("Draft", 6, 5, 3, 11, 4 + data.indexInRound), data.pokemon)
    }

    override suspend fun RequestBuilder.banDoc(data: BanData) {
        addSingle(Coord("Draft", "AB", 15 + data.indexInRound), data.pokemon)
    }

    override fun executeTipGameSending(num: Int) {
        launch {
            val tip = tipgame!!
            val channel = jda.getTextChannelById(tip.channel)!!
            val matchups = getMatchups(num)
            val names = jda.getGuildById(guild)!!.retrieveMembersByIds(matchups.flatten()).await()
                .associate { it.idLong to it.user.effectiveName }
            channel.send(
                embeds = Embed(
                    title = "Spieltag $num",
                    color = when (leagueNameRegex.find(leaguename)?.groupValues[1]) {
                        "Rot" -> 0xFF0000
                        "Blau" -> 0x0000FF
                        "Gold" -> 0xFFD700
                        "Silber" -> 0xC0C0C0
                        else -> Color.YELLOW.rgb
                    }
                ).into()
            ).queue()
            for ((index, matchup) in matchups.withIndex()) {
                val u1 = matchup[0]
                val u2 = matchup[1]
                val base: ArgBuilder<TipGameManager.VoteButton.Args> = {
                    this.leaguename = this@EPP.leaguename
                    this.gameday = num
                    this.index = index
                }
                val messageId = channel.send(
                    embeds = Embed(
                        title = "${names[u1]} vs. ${names[u2]}", color = embedColor,
                        description = if (tip.withCurrentState) "Bisherige Votes: 0:0" else null
                    ).into(), components = ActionRow.of(TipGameManager.VoteButton(names[u1]!!) {
                        base()
                        this.userindex = u1.indexedBy(table)
                    }, TipGameManager.VoteButton(names[u2]!!) {
                        base()
                        this.userindex = u2.indexedBy(table)
                    }).into()
                ).await().idLong
                TipGameMessagesDB.set(leaguename, num, index, messageId)
            }
        }
    }

    override suspend fun executeYoutubeSend(
        ytTC: Long,
        gameday: Int,
        battle: Int,
        strategy: VideoProvideStrategy,
        overrideEnabled: Boolean
    ) {
        val b = builder()
        val ytVideoSaveData = replayDataStore?.data?.get(gameday)?.get(battle)?.ytVideoSaveData
        if (!overrideEnabled && ytVideoSaveData?.enabled != true) return logger.info("ExecuteYTSend: Not enabled")
        ytVideoSaveData?.enabled = false
        jda.getTextChannelById(ytTC)!!.sendMessage(buildString {
            append("<@&818198125755498598>\n")
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


    init {
        /*enableConfig(
            DraftBanConfig(
                banRounds = mapOf(
                    1 to BanRoundConfig.FixedTierSet(
                        mapOf("S" to 1, "A" to 1, "B" to 2, "C" to 2, "D" to 2)
                    )
                ),
                notBannable = setOf(
                    "Epitaff",
                    "Qurtel",
                    "Rotom-Wash",
                    "Suelord",
                    "Florges",
                    "Quaxo",
                    "Folipurba",
                    "Zapplarang",
                    "Sengo",
                    "Skorgla",
                    "Spinsidias"
                ),
                skipBehavior = BanSkipBehavior.RANDOM
            )
        )*/
    }
}
