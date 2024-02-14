package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.features.draft.AddToTierlistData
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.x
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.litote.kmongo.SetTo
import org.litote.kmongo.setTo

@Serializable
@SerialName("IPL")
class IPL(val draftSheetId: Int, var actuallyPicked: Int = 0) : League() {
    override val teamsize = 12
    override val pickBuffer = 5

    @Transient
    private val timerSkipMode = MovePicksMode(4) { roundNum ->
        builder().addColumn("Draft- und Moderation!${roundNum.minus(1).x(1, 3)}5", order[roundNum]!!.map {
            "=" + it.firstMonCoord().plusY(-1)
        }).execute()
    }

    private fun Int.firstMonCoord() = CoordXMod("Kaderübersicht", 4, 'J' - 'D', 4, 17, 6)

    @Transient
    override val afterTimerSkipMode = timerSkipMode

    @Transient
    override val duringTimerSkipMode = timerSkipMode

    override suspend fun AddToTierlistData.addMonToTierlist() {
        val data = pkmn.await()
        builder().addRow("Data!K${index + 600}", listOf(mon, data.getIcon(), data.speed, tier)).execute()
    }

    override fun reset(updates: MutableList<SetTo<*>>) {
        updates += IPL::actuallyPicked setTo 0
    }

    override fun onRoundSwitch() {
        actuallyPicked = 0
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(data.memIndex.firstMonCoord().plusY(data.changedOnTeamsiteIndex), data.pokemon)
        actuallyPicked++
        addStrikethroughChange(draftSheetId, round + 2, actuallyPicked + 4, true)
    }
}
