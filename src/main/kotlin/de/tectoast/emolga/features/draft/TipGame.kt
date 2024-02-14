package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.defaultScope
import de.tectoast.emolga.utils.defaultTimeFormat
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.indexedBy
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.dv8tion.jda.api.interactions.components.ActionRow
import org.litote.kmongo.eq
import java.awt.Color
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

    fun executeTipGameSending(league: League, num: Int) {
        defaultScope.launch {
            val docEntry = league.docEntry!!
            val tip = league.tipgame!!
            val channel = EmolgaMain.emolgajda.getTextChannelById(tip.channel)!!
            val matchups = docEntry.getMatchups(num)
            val names =
                EmolgaMain.emolgajda.getGuildById(league.guild)!!.retrieveMembersByIds(matchups.flatten()).await()
                    .associate { it.idLong to it.effectiveName }
            val table = league.table
            channel.send(
                embeds = Embed(
                    title = "Spieltag $num", color = Color.YELLOW.rgb
                ).into()
            ).queue()
            for ((index, matchup) in matchups.withIndex()) {
                val u1 = matchup[0]
                val u2 = matchup[1]
                val baseid = "tipgame;${league.leaguename}:$num:$index"
                channel.send(
                    embeds = Embed(
                        title = "${names[u1]} vs. ${names[u2]}", color = embedColor
                    ).into(), components = ActionRow.of(
                        primary("$baseid:${u1.indexedBy(table)}", names[u1]),
                        primary("$baseid:${u2.indexedBy(table)}", names[u2]),
                    ).into()
                ).queue()
            }
        }
    }

    fun executeTipGameLockButtons(league: League, gameday: Int) {
        defaultScope.launch {
            EmolgaMain.emolgajda.getTextChannelById(league.tipgame!!.channel)!!.iterableHistory.takeAsync(league.table.size / 2)
                .await().forEach {
                    it.editMessageComponents(
                        ActionRow.of(it.actionRows[0].buttons.map { button -> button.asDisabled() })
                    ).queue()
                }
            league.onTipGameLockButtons(gameday)
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
object DateToStringSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(defaultTimeFormat.format(value))
    }

    override fun deserialize(decoder: Decoder): Date {
        return defaultTimeFormat.parse(decoder.decodeString())
    }
}

@Serializable
class TipGamedayData(
    val userdata: MutableMap<Long, MutableMap<Int, Int>> = mutableMapOf(),
    val evaluated: MutableList<Int> = mutableListOf()
)