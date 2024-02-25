package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.TipGameUserData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.IPL
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.SelectOption
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
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
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
            val league = db.getLeague(e.leaguename) ?: return reportMissing()
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

    object RankSelect : SelectMenuFeature<RankSelect.Args>(::Args, SelectMenuSpec("rankselect")) {
        class Args : Arguments() {
            var league by league().compIdOnly()
            var rank by int().compIdOnly()
            var user by singleOption()
        }

        suspend fun createFromLeague(league: League, rank: Int): StringSelectMenu {
            val names = jda.getGuildById(league.guild)!!.retrieveMembersByIds(league.table).await()
                .associate { it.idLong to it.user.effectiveName }
            return this("Platz $rank", options = league.table.mapIndexed { index, user ->
                if (league is IPL) SelectOption(
                    league.teamtable[index],
                    index.toString(),
                    emoji = Emoji.fromFormatted(league.emotes[index])
                )
                else SelectOption(names[user]!!, index.toString())
            }) {
                this.league = league
                this.rank = rank
            }
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            ephemeralDefault()
            val league = e.league
            val userindex = e.user.toInt()
            TipGameUserData.setOrderGuess(user, league.leaguename, e.rank, userindex)
            reply("<@${league.table[userindex]}> wurde auf deiner Liste auf Platz **${e.rank}** gesetzt!")
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
    @Serializable(with = DurationSerializer::class)
    val interval: Duration,
    val amount: Int,
    val channel: Long
)

object InstantToStringSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

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
