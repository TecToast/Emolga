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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
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
            deferReply()
            val league = db.getLeague(e.leaguename) ?: return reportMissing()
            league.lock {
                val tipgame = league.tipgame ?: return reportMissing()
                val gamedayMap = tipgame.tips.getOrPut(e.gameday) { mutableMapOf() }
                val userMap = gamedayMap.getOrPut(user) { mutableMapOf() }
                userMap[e.index] = e.userindex
                reply("Dein Tipp wurde gespeichert!")
                if (tipgame.withCurrentState) {
                    message.editMessageEmbeds(
                        Embed(title = message.embeds[0].title,
                            description = "Bisherige Votes: " + league.battleorder(e.gameday)[e.index].joinToString(":") { u ->
                                gamedayMap.values.count { u in it.values }.toString()
                            }, color = embedColor
                        )
                    ).queue()
                }
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
                    league.teamtable[index], index.toString(), emoji = Emoji.fromFormatted(league.emotes[index])
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
}

@Serializable
class TipGame(
    val tips: MutableMap<Int, MutableMap<Long, MutableMap<Int, Int>>> = mutableMapOf(),
    @Serializable(with = InstantToStringSerializer::class) val lastSending: Instant,
    @Serializable(with = InstantToStringSerializer::class) val lastLockButtons: Instant? = null,
    @Serializable(with = DurationSerializer::class) val interval: Duration,
    val amount: Int,
    val channel: Long,
    val withCurrentState: Boolean = false
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
