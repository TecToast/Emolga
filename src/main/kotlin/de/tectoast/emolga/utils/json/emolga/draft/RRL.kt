package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.features.TestInteractionData
import de.tectoast.emolga.features.draft.NuzlockeCommand
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.showdown.PlayerSaveKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("RRL")
class RRL(val rerollChannel: Long) : League() {
    override val teamsize = 11

    @Transient
    override var timer: DraftTimer? = SimpleTimer(TimerInfo(9, 22, delayInMins = 60))
    override val afterTimerSkipMode = AFTER_DRAFT_ORDERED
    override val duringTimerSkipMode = NEXT_PICK

    val division by lazy { leaguename.last().digitToInt() }


    override val dataSheet: String
        get() = "Data$division"


    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(
            data.memIndex.coordXMod(
                "Kader", 2, 5, division.y('P' - 'C', 4), 34, 25 + data.changedOnTeamsiteIndex
            ), data.pokemon
        )
        addStrikethroughChange(
            340699480, "${(data.roundIndex + 3).xc()}${division.y(21 - 4, 6 + data.indexInRound)}", strikethrough = true
        )
    }

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(
            SorterData(
                listOf("Tabelle!D6:K13", "Tabelle!D18:K25"), newMethod = true, cols = listOf(3, 7, 5)
            )
        ) {
            b.addSingle(
                if (gdi == 6) "Spielplan!${division.x('N' - 'C', 6)}${35 + index}"
                else gdi.coordYMod("Spielplan", 3, 4, division.y('N' - 'C', 4), 9, 8 + index), defaultGameplanString
            )
        }
    }

    override suspend fun onReplayAnalyse(data: ReplayData) {
        if (rrlDisableAutoReroll) return
        for (i in data.uids.indices) {
            val sdPlayer = data.game[i].sdPlayer ?: continue
            val mon = sdPlayer[PlayerSaveKey.FIRST_FAINTED] ?: continue
            with(rerollInteractionData) {
                if (sdPlayer.containsZoro()) {
                    sendMessage("Im Team von ${sdPlayer.nickname} befindet sich ein Pokemon mit Illusion, zur Sicherheit wurde der automatische Reroll abgebrochen.")
                    return
                }
                val uid = data.uids[i]
                NuzlockeCommand.executeMonSwitch(uid, mon.draftname)
            }
        }
    }

    private val rerollInteractionData
        get() = TestInteractionData(
            tc = rerollChannel,
            gid = Constants.G.HELBIN,
            sendReplyInTc = true
        )

}

var rrlDisableAutoReroll = false
