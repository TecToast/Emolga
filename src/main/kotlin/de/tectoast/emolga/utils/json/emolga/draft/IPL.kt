@file:OptIn(ExperimentalSerializationApi::class)

package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.ArgBuilder
import de.tectoast.emolga.features.draft.AddToTierlistData
import de.tectoast.emolga.features.draft.TipGameManager
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.records.SorterData
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake
import net.dv8tion.jda.api.interactions.components.ActionRow
import java.awt.Color

@Serializable
@SerialName("IPL")
class IPL(
    private val draftSheetId: Int,
    @EncodeDefault var pickTries: Int = 0,
    val teamtable: List<String> = listOf(),
    val emotes: List<String> = listOf()
) : League() {
    override val teamsize = 12
    override val pickBuffer = 5

    override val alwaysSendTier = true

    val isYT by lazy { !leaguename.endsWith("C") }

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(SorterData("Tabelle!C3:I10", newMethod = true, cols = listOf(6, 5, -1))) {
            b.addRow(
                gdi.coordXMod("Spielplan (SPOILER)", 3, 'J' - 'B', 4, 8, 5 + index),
                defaultSplitGameplanStringWithoutUrl
            )
        }
    }

    object MovePicksMode : DuringTimerSkipMode, AfterTimerSkipMode {
        private const val TURNS = 4
        override suspend fun League.afterPickCall(data: NextPlayerData) = afterPick(data)

        override suspend fun League.afterPick(data: NextPlayerData): Boolean {
            if (this is IPL) {
                when (data) {
                    is NextPlayerData.Moved -> {
                        val curIndex = table.indexOf(current) // 6
                        var insertIndex = TURNS
                        val currentOrder = order[round]!!
                        val size = currentOrder.size
                        var roundToInsert = round // 1 -> 2
                        var isNextRound = false
                        if (insertIndex == size) insertIndex++
                        if (insertIndex > size) {
                            insertIndex -= size
                            roundToInsert++
                            isNextRound = true
                        }
                        if (roundToInsert > totalRounds) {
                            roundToInsert = totalRounds
                            insertIndex = order[roundToInsert]!!.size
                            isNextRound = false
                        }
                        val orderRoundToInsert = order[roundToInsert]!!
                        orderRoundToInsert.add(insertIndex, curIndex)

                        val b = builder()
                        val sheetName = "Draftreihenfolge"
                        val rTIIndex = roundToInsert - 1
                        val rIndex = round - 1
                        b.addSingle(Coord(sheetName, rIndex.x(1, 3), 5 + pickTries), "")
                        if (isNextRound) {
                            b.addColumn(
                                Coord(sheetName, rTIIndex.x(1, 3), 5), orderRoundToInsert.mapToPlayers()
                            )
                        } else {
                            b.addColumn(
                                Coord(sheetName, rTIIndex.x(1, 3), 6 + pickTries), orderRoundToInsert.mapToPlayers()
                            )
                        }
                        b.execute()
                    }

                    NextPlayerData.Normal -> {
                        if (hasMovedTurns()) movedTurns().removeFirst()
                    }
                }
                pickTries++
            } else {
                error("Not IPL")
            }
            return true
        }

        context(IPL)
        private fun List<Int>.mapToPlayers() = map {
            "=" + it.firstMonCoord().plusY(-1)
        }

        override suspend fun League.getPickRound() = movedTurns().firstOrNull() ?: round
    }

    fun Int.firstMonCoord() = CoordXMod("Kaderübersicht", 4, 'J' - 'D', 4, 17, 6)

    @Transient
    override val afterTimerSkipMode = MovePicksMode

    @Transient
    override val duringTimerSkipMode = MovePicksMode

    override suspend fun AddToTierlistData.addMonToTierlist() {
        val data = pkmn.await()
        builder().addRow("Data!K${index + 600}", listOf(mon, data.getIcon(), data.speed, tier)).execute()
    }

    override fun reset() {
        pickTries = 0
    }

    override suspend fun onRoundSwitch() {
        pickTries = 0
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(data.memIndex.firstMonCoord().plusY(data.changedOnTeamsiteIndex), data.pokemon)
        addStrikethroughChange(draftSheetId, round + 2, pickTries + 5, true)
    }

    override suspend fun handleStallSecondUsed(): Long {
        return tc.sendMessage(
            "${getCurrentMention()} Dein Uhrsaring-Zuschlag läuft! Du wirst <t:${timerRelated.cooldown / 1000}:R> geskippt!"
        ).setStickers(
            StickerSnowflake.fromId(1207743104837492756)
        ).await().idLong
    }

    override fun NextPlayerData.Moved.sendSkipMessage() {
        if (reason == SkipReason.SKIP) tc.sendMessage("${getCurrentName(skippedUser)} wurde geskippt!").queue()
        else tc.sendMessage("<@$skippedUser> Dein Uhrsaring-Zuschlag ist abgelaufen. Du wirst geskippt!")
            .setStickers(StickerSnowflake.fromId(1207743822826836061)).queue()
    }

    override suspend fun onNextPlayer(data: NextPlayerData): Unit = with(timerRelated) {
        lastStallSecondUsedMid?.takeIf { it > 0 }?.let {
            tc.editMessageById(
                it, "<@$current> Dein Uhrsaring-Zuschlag ${
                    if (data is NextPlayerData.Normal) "beträgt noch ${
                        TimeUtils.secondsToTimePretty((cooldown - System.currentTimeMillis()) / 1000)
                    }!"
                    else "wurde vollständig aufgebraucht!"
                }"
            ).queue()
        }
    }

    override fun executeTipGameSending(num: Int) {
        launch {
            val tip = tipgame!!
            val channel = jda.getTextChannelById(tip.channel)!!
            val matchups = getMatchupsIndices(num)
            channel.send(
                content = "<@&878744967680512021>", embeds = Embed(
                    title = "Spieltag $num", color = Color.YELLOW.rgb
                ).into()
            ).queue()
            for ((index, matchup) in matchups.withIndex()) {
                val u1 = matchup[0]
                val u2 = matchup[1]
                val base: ArgBuilder<TipGameManager.VoteButton.Args> = {
                    this.leaguename = this@IPL.leaguename
                    this.gameday = num
                    this.index = index
                }
                val t1 = teamtable[u1]
                val t2 = teamtable[u2]
                channel.send(
                    embeds = Embed(
                        title = "$t1 ${emotes[u1]} vs. ${emotes[u2]} $t2", color = embedColor
                    ).into(),
                    components = ActionRow.of(TipGameManager.VoteButton(t1, emoji = Emoji.fromFormatted(emotes[u1])) {
                        base()
                        this.userindex = u1
                    }, TipGameManager.VoteButton(t2, emoji = Emoji.fromFormatted(emotes[u2])) {
                        base()
                        this.userindex = u2
                    }).into()
                ).queue()
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
            if (battle == 0 || battle == 3) append("<@&878744967680512021>\n")
            append("**Spieltag $gameday**\n_Kampf ${battle + 1}_\n\n")
            val muData = battleorder[gameday]!![battle]
            append(muData.joinToString(" vs. ") { emotes[it] })
            append("\n\n")
            val videoIds = muData.mapIndexed { index, uindex ->
                strategy.run { provideVideoId(index, uindex) }?.let { videoId ->
                    val range =
                        gameday.minus(1)
                            .CoordXMod("Spielplan (SPOILERFREI)", 3, 'J' - 'B', 3 + index * 3, 8, 5 + battle)
                    b.addSingle(
                        range,
                        "=HYPERLINK(\"https://www.youtube.com/watch?v=$videoId\"; \"Kampf\nanschauen\")"
                    )
                    b.addFGColorChange(216749258, range.x, range.y, 0x1155cc.convertColor())
                    videoId
                }
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
