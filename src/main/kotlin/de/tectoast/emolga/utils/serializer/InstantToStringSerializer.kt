package de.tectoast.emolga.utils.serializer

import de.tectoast.emolga.utils.parseToInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import mu.KotlinLogging
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

object InstantToStringSerializer : KSerializer<Instant> {
    private val logger = KotlinLogging.logger {}
    private val format: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantToString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(
            format.format(
                value.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
            )
        )
    }

    override fun deserialize(decoder: Decoder): Instant {
        val decodedString = decoder.decodeString()
        return runCatching { format.parseToInstant(decodedString) }.onFailure {
            logger.error("Failed to parse Instant from string: $decodedString", it)
        }.getOrThrow()
    }
}
