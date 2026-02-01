@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.exposed.TipGameVotesDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.messages.Embed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object TipGameManager : CoroutineScope {
    override val coroutineContext = createCoroutineContext("TipGameManager", Dispatchers.IO)

    object VoteButton : ButtonFeature<VoteButton.Args>(::Args, ButtonSpec("tipgame")) {
        class Args : Arguments() {
            var leaguename by string()
            var gameday by int()
            var index by int()
            var userindex by int()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.ephemeralDefault()
            iData.deferReply()
            val league = db.getLeague(e.leaguename) ?: return reportMissing()
            val tipgame = league.config.tipgame ?: return reportMissing()
            TipGameVotesDB.addVote(iData.user, e.leaguename, e.gameday, e.index, e.userindex)
            iData.reply("Dein Tipp wurde gespeichert!")
            if (tipgame.currentState == TipGameCurrentStateType.ALWAYS) {
                iData.message.editMessageEmbeds(
                    Embed(
                        title = iData.message.embeds[0].title,
                        description = league.buildCurrentState(e.gameday, e.index),
                        color = embedColor
                    )
                ).queue()
            }
        }

        context(iData: InteractionData)
        private fun reportMissing() {
            iData.reply("Dieses Tippspiel existiert nicht mehr!")
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
    val roleToPing: Long? = null,
    val withName: String? = null,
    val currentState: TipGameCurrentStateType? = null
)

@Serializable
enum class TipGameCurrentStateType {
    ALWAYS,
    ON_LOCK
}

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
