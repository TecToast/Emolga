package de.tectoast.emolga.utils.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import mu.KotlinLogging

object ColorToStringSerializer : KSerializer<Int> {
    private val logger = KotlinLogging.logger {}
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ColorToString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeString(String.format("#%06X", 0xFFFFFF and value))
    }

    override fun deserialize(decoder: Decoder): Int {
        val decodedString = decoder.decodeString()
        return runCatching { decodedString.removePrefix("#").toInt(16) }.onFailure {
            logger.error("Failed to parse color from string: $decodedString", it)
        }.getOrThrow()
    }
}