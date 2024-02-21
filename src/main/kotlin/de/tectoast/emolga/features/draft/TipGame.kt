package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.createCoroutineContext
import de.tectoast.emolga.utils.defaultTimeFormat
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.indexedBy
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
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
import kotlin.time.Duration

object TipGameManager : CoroutineScope {
    override val coroutineContext = createCoroutineContext("TipGameManager", Dispatchers.IO)
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
            league.lock {
                val tipgame = league.tipgame ?: return reportMissing()
                val usermap =
                    tipgame.tips.getOrPut(e.gameday) { TipGamedayData() }.userdata.getOrPut(user) { mutableMapOf() }
                usermap[e.index] = e.userindex
                reply("Dein Tipp wurde gespeichert!")
                league.save()
            }
        }

        context(InteractionData)
        private fun reportMissing() {
            reply("Dieses Tippspiel existiert nicht mehr!")
        }
    }

    fun executeTipGameSending(league: League, num: Int) {
        launch {
            val tip = league.tipgame!!
            val channel = jda.getTextChannelById(tip.channel)!!
            val matchups = league.getMatchups(num)
            val names =
                jda.getGuildById(league.guild)!!.retrieveMembersByIds(matchups.flatten()).await()
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
                val base: ArgBuilder<VoteButton.Args> = {
                    this.leaguename = league.leaguename
                    this.gameday = num
                    this.index = index
                }
                channel.send(
                    embeds = Embed(
                        title = "${names[u1]} vs. ${names[u2]}", color = embedColor
                    ).into(), components = ActionRow.of(VoteButton(names[u1]!!) {
                        base()
                        this.userindex = u1.indexedBy(table)
                    }, VoteButton(names[u2]!!) {
                        base()
                        this.userindex = u2.indexedBy(table)
                    }).into()
                ).queue()
            }
        }
    }

    fun executeTipGameLockButtons(league: League, gameday: Int) {
        launch {
            jda.getTextChannelById(league.tipgame!!.channel)!!.iterableHistory.takeAsync(league.battleorder[gameday]!!.size)
                .await().forEach {
                    it.editMessageComponents(
                        ActionRow.of(it.actionRows[0].buttons.map { button -> button.asDisabled() })
                    ).queue()
                }
        }
    }

    fun executeTipGameLockButtonsIndividual(league: League, gameday: Int, mu: Int) {
        launch {
            val muCount = league.battleorder[gameday]!!.size
            jda.getTextChannelById(league.tipgame!!.channel)!!.iterableHistory.takeAsync(muCount - mu).await().last()
                .let {
                    it.editMessageComponents(
                        ActionRow.of(it.actionRows[0].buttons[mu].asDisabled())
                    ).queue()
                }
        }
    }
}

@Serializable
class TipGame(
    val tips: MutableMap<Int, TipGamedayData> = mutableMapOf(),
    @Serializable(with = InstantToStringSerializer::class) val lastSending: Instant,
    @Serializable(with = InstantToStringSerializer::class) val lastLockButtons: Instant?,
    val interval: Duration,
    val amount: Int,
    val channel: Long
)

object InstantToStringSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(defaultTimeFormat.format(value.toEpochMilliseconds()))
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.fromEpochMilliseconds(defaultTimeFormat.parse(decoder.decodeString()).time)
    }
}

@Serializable
class TipGamedayData(
    val userdata: MutableMap<Long, MutableMap<Int, Int>> = mutableMapOf(),
    val evaluated: MutableList<Int> = mutableListOf()
)
