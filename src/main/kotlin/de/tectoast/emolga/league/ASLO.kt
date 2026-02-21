package de.tectoast.emolga.league

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.league.AddToTierlistData
import de.tectoast.emolga.utils.DocEntry
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.coordXMod
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.records.TableSortOption
import de.tectoast.emolga.utils.records.newSystemSorter
import de.tectoast.emolga.utils.records.toCoord
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import de.tectoast.emolga.utils.y
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.JDA
import kotlin.time.Duration.Companion.days

@Serializable
@SerialName("ASLO")
class ASLO(
    val replayChannel: Long
) : League() {

    val conf = leaguename.last()
    val confidx = conf - 'A'

    override val dataSheet = "Data${if (confidx == 0) "Verkehr" else "Leiter"}"
    override val pickBuffer = 15
    override val teamsize = 12
    override val gamedays = 7

    override fun getTierlistFor(idx: Int): Tierlist {
        return Tierlist[guild, when (idx) {
            0 -> "KantoJohto"
            1 -> "Hoenn"
            2 -> "Sinnoh"
            3 -> "Einall"
            4 -> "Kalos"
            5 -> "Alola"
            6 -> "Galar"
            7 -> "Paldea"
            else -> error("Invalid index")
        }]!!
    }

    @Transient
    override val docEntry = DocEntry.create(this) {
        val yBase = confidx.y(11, 4)
        newSystem(
            newSystemSorter(
                formulaRange = "Tabelle!C${yBase}:K${yBase + 7}",
                sortOptions = TableSortOption.fromCols(listOf(8, -1, 7))
            ), resultCreator = {
                b.addSingle(
                    (if (gdi.mod(5) in 0..1) gdi.coordXMod(
                        "Spielplan",
                        2,
                        4,
                        5,
                        0,
                        (gdi / 5).y(12, confidx.y(20, 5 + index))
                    )
                    else gdi.minus(2).coordXMod("Spielplan", 3, 4, 3, 0, confidx.y(20, 11 + index))).toCoord(),
                    defaultGameplanString
                )
            })
    }

    override fun provideReplayChannel(jda: JDA) = jda.getTextChannelById(replayChannel)
    override fun provideResultChannel(jda: JDA) = jda.getTextChannelById(resultChannel!!)

    override suspend fun AddToTierlistData.addMonToTierlist() {
        val poke = pkmn.await()
        builder().addRow(
            "Data!B${index() + 600}", listOf(mon, tier, poke.getGen5SpriteFormula(), poke.speed, englishTLName.await())
        ).execute()
    }


    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED
    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(
            data.roundIndex.coordXMod("Draft $conf", 6, 4, 3, 10, 4 + data.indexInRound), data.pokemon
        )
    }

    override suspend fun RequestBuilder.switchDoc(data: SwitchData) {
        newSystemSwitchDoc(data)
        addRow(
            data.roundIndex.coordXMod("PlayOffs-Draft $conf", 3, 4, 3, 5, data.indexInRound + 4),
            listOf(data.pokemon, data.oldmon.tlName)
        )
    }

    override fun setupCustomRepeatTasks() {
        if (confidx == 0) {
            RepeatTask(
                "ASL", RepeatTaskType.Other("Announce"), "26.05.2025 00:00", 8, 7.days
            ) {
                executeGamedaySending(it)
            }
        }
    }

    fun executeGamedaySending(gameday: Int) {
        val msg = "**------------- Spieltag $gameday -------------**"
        jda.getTextChannelById(replayChannel)!!.sendMessage(msg).queue()
        jda.getTextChannelById(resultChannel!!)!!.sendMessage(msg).queue()
    }


}
