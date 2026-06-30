package de.tectoast.emolga.utils.serializer

import de.tectoast.emolga.domain.util.service.TimeFormatService
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration


object DurationSerializer : KSerializer<Duration>, KoinComponent {
    override val descriptor = PrimitiveSerialDescriptor("Interval", PrimitiveKind.STRING)
    private val formatService by inject<TimeFormatService>()
    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeString(formatService.durationToPrettyShort(value))
    }

    override fun deserialize(decoder: Decoder): Duration {
        return formatService.parseDuration(decoder.decodeString())
    }
}
