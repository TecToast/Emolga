package de.tectoast.emolga.utils.serializer

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.toShowdownID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ShowdownIDSerializer : KSerializer<ShowdownID> {
    override val descriptor = PrimitiveSerialDescriptor("ShowdownID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ShowdownID) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ShowdownID {
        val decoded = decoder.decodeString()
        val showdownID = decoded.toShowdownID()
        if (showdownID.value != decoded) {
            throw IllegalArgumentException("Invalid ShowdownID format: $decoded")
        }
        return showdownID
    }
}