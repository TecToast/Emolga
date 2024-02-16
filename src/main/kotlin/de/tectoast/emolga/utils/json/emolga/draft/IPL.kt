package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.features.draft.AddToTierlistData
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.records.SorterData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake
import org.litote.kmongo.SetTo
import org.litote.kmongo.set
import org.litote.kmongo.setTo

@Serializable
@SerialName("IPL")
class IPL(private val draftSheetId: Int, var pickTries: Int = 0) : League() {
    override val teamsize = 12
    override val pickBuffer = 5

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(SorterData("C3:I10", newMethod = true, cols = listOf(6, 5, -1))) {
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
                        val orderRoundToInsert = order[roundToInsert]!!
                        if (roundToInsert > totalRounds) {
                            roundToInsert = totalRounds
                            insertIndex = orderRoundToInsert.size
                        }
                        orderRoundToInsert.add(insertIndex, curIndex)

                        val b = builder()
                        val sheetName = "Draft- und Moderation"
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

    override fun reset(updates: MutableList<SetTo<*>>) {
        updates += IPL::pickTries setTo 0
    }

    override suspend fun onRoundSwitch() {
        println("Updating pickTries")
        pickTries = 0
        db.drafts.updateOneById(id!!, set(IPL::pickTries setTo 0))
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(data.memIndex.firstMonCoord().plusY(data.changedOnTeamsiteIndex), data.pokemon)
        addStrikethroughChange(draftSheetId, round + 2, pickTries + 5, true)
    }

    override suspend fun handleStallSecondUse() {
        tc.sendMessage(
            getCurrentMention() + " Dein Uhrsaring-Zuschlag läuft! Du hast noch ${
                TimeUtils.convertToMinsSecs(
                    currentlyUsedStallSeconds()
                )
            } Zeit bis du geskippt wirst!"
        ).setStickers(
            StickerSnowflake.fromId(1207743104837492756)
        ).queue()
    }

    override fun NextPlayerData.Moved.sendSkipMessage() {
        if (reason == SkipReason.SKIP) tc.sendMessage("${getCurrentName(skippedUser)} wurde geskippt!").queue()
        tc.sendMessage("<@$skippedUser> Dein Uhrsaring-Zuschlag ist abgelaufen. Du wirst geskippt!")
            .setStickers(StickerSnowflake.fromId(1207743822826836061)).queue()
    }
}
