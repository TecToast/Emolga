package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.TipGameUserData
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.messages.Embed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.keyProjection
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
            deferReply()
            val league = db.getLeague(e.leaguename) ?: return reportMissing()
            val tipgame = league.config.tipgame ?: return reportMissing()
            TipGameUserData.addVote(user, e.leaguename, e.gameday, e.index, e.userindex)
            reply("Dein Tipp wurde gespeichert!")
            if (tipgame.withCurrentState) {
                message.editMessageEmbeds(
                    Embed(
                        title = message.embeds[0].title,
                        description = "Bisherige Votes: " + league.battleorder(e.gameday)[e.index].map {
                            db.tipgameuserdata.countDocuments(
                                and(
                                    TipGameUserData::league eq e.leaguename,
                                    TipGameUserData::tips.keyProjection(e.gameday).keyProjection(e.index) eq it
                                )
                            ).toString()
                        }.joinToString(":"),
                        color = embedColor
                    )
                ).queue()
            }
        }

        context(InteractionData)
        private fun reportMissing() {
            reply("Dieses Tippspiel existiert nicht mehr!")
        }
    }
}

@Serializable
data class TipGame(
    @Serializable(with = InstantToStringSerializer::class) val lastSending: Instant,
    @Serializable(with = InstantToStringSerializer::class) val lastLockButtons: Instant? = null,
    @Serializable(with = DurationSerializer::class) val interval: Duration,
    val amount: Int,
    val channel: Long,
    val colorConfig: TipGameColorConfig = TipGameColorConfig.Default,
    val withCurrentState: Boolean = false
)

@Serializable
sealed interface TipGameColorConfig {
    suspend fun provideEmbedColor(league: League): Int

    @Serializable
    @SerialName("Default")
    object Default : TipGameColorConfig {
        override suspend fun provideEmbedColor(league: League): Int {
            return Color.YELLOW.rgb
        }
    }

    @Serializable
    @SerialName("Fixed")
    data class Fixed(val color: Int) : TipGameColorConfig {
        override suspend fun provideEmbedColor(league: League): Int {
            return color
        }
    }

    @Serializable
    @SerialName("RGB")
    data class RGB(val rgb: String) : TipGameColorConfig {
        override suspend fun provideEmbedColor(league: League): Int {
            return rgb.replace("#", "").toInt(16)
        }
    }


}

object InstantToStringSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantToString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(defaultTimeFormat.format(value.toEpochMilliseconds()))
    }

    override fun deserialize(decoder: Decoder): Instant {
        val decodedString = decoder.decodeString()
        return runCatching { Instant.fromEpochMilliseconds(defaultTimeFormat.parse(decodedString).time) }.onFailure {
            universalLogger.error("Failed to parse Instant from string: $decodedString", it)
        }.getOrThrow()
    }
}
