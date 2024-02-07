package de.tectoast.emolga.features.draft

import de.tectoast.emolga.commands.DateToStringSerializer
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import java.util.*

object TipGameManager {
    object VoteButton : ButtonFeature<VoteButton.Args>(::Args, ButtonSpec("tipgame")) {
        class Args : Arguments() {
            var leaguename by string()
            var gameday by int()
            var index by int()
            var userindex by int()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            ephemeralDefault()
            val league = db.drafts.findOne(League::leaguename eq e.leaguename) ?: return reportMissing()
            val tipgame = league.tipgame ?: return reportMissing()
            val usermap =
                tipgame.tips.getOrPut(e.gameday) { TipGamedayData() }.userdata.getOrPut(user) { mutableMapOf() }
            usermap[e.index] = e.userindex
            reply("Dein Tipp wurde gespeichert!")
            league.save()
        }

        context(InteractionData)
        private fun reportMissing() {
            reply("Dieses Tippspiel existiert nicht mehr!")
        }
    }
}

@Serializable
class TipGame(
    val tips: MutableMap<Int, TipGamedayData> = mutableMapOf(),
    @Serializable(with = DateToStringSerializer::class)
    val lastSending: Date,
    @Serializable(with = DateToStringSerializer::class)
    val lastLockButtons: Date,
    val interval: String,
    val amount: Int,
    val channel: Long
)

@Serializable
class TipGamedayData(
    val userdata: MutableMap<Long, MutableMap<Int, Int>> = mutableMapOf(),
    val evaluated: MutableList<Int> = mutableListOf()
)
