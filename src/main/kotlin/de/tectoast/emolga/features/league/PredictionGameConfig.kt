@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.features.league

import de.tectoast.emolga.database.exposed.PredictionGameVotesDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.DurationSerializer
import de.tectoast.emolga.utils.defaultTimeFormat
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.universalLogger
import dev.minn.jda.ktx.messages.Embed
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

object PredictionGameManager {

    object VoteButton :
        ButtonFeature<VoteButton.Args>(::Args, ButtonSpec("predictiongame").apply { aliases += "tipgame" }) {
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
            val league = mdb.getLeague(e.leaguename) ?: return reportMissing()
            val predictionGame = league.config.predictionGame ?: return reportMissing()
            PredictionGameVotesDB.addVote(iData.user, e.leaguename, e.gameday, e.index, e.userindex)
            iData.reply(K18n_PredictionGame.PredictionSaved)
            if (predictionGame.currentState == PredictionGameCurrentStateType.ALWAYS) {
                iData.message.editMessageEmbeds(
                    Embed(
                        title = iData.message.embeds[0].title,
                        description = league.buildCurrentPredictionGameState(e.gameday, e.index),
                        color = embedColor
                    )
                ).queue()
            }
        }

        context(iData: InteractionData)
        private fun reportMissing() {
            iData.reply(K18n_PredictionGame.PredictionGameMissing)
        }
    }
}

@Serializable
data class PredictionGameConfig(
    @Serializable(with = InstantToStringSerializer::class) val lastSending: Instant,
    @Serializable(with = InstantToStringSerializer::class) val lastLockButtons: Instant? = null,
    @Serializable(with = DurationSerializer::class) val interval: Duration,
    val amount: Int,
    val channel: Long,
    @Serializable(with = ColorToStringSerializer::class)
    val customEmbedColor: Int? = null,
    val roleToPing: Long? = null,
    val currentState: PredictionGameCurrentStateType? = null,
    val updateConfig: PredictionGameUpdateConfig? = null
)

@Serializable
data class PredictionGameUpdateConfig(
    val channel: Long,
    val topN: Int
)

@Serializable
enum class PredictionGameCurrentStateType {
    ALWAYS,
    ON_LOCK
}

object ColorToStringSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ColorToString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeString(String.format("#%06X", 0xFFFFFF and value))
    }

    override fun deserialize(decoder: Decoder): Int {
        val decodedString = decoder.decodeString()
        return runCatching { decodedString.removePrefix("#").toInt(16) }.onFailure {
            universalLogger.error("Failed to parse color from string: $decodedString", it)
        }.getOrThrow()
    }
}

@Serializable
sealed interface PredictionGameColorConfig {
    suspend fun provideEmbedColor(league: League): Int

    @Serializable
    @SerialName("Default")
    object Default : PredictionGameColorConfig {
        override suspend fun provideEmbedColor(league: League): Int {
            return Color.YELLOW.rgb
        }
    }

    @Serializable
    @SerialName("Fixed")
    data class Fixed(val color: Int) : PredictionGameColorConfig {
        override suspend fun provideEmbedColor(league: League): Int {
            return color
        }
    }

    @Serializable
    @SerialName("RGB")
    data class RGB(val rgb: String) : PredictionGameColorConfig {
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
