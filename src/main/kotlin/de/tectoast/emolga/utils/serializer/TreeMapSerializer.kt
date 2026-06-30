package de.tectoast.emolga.utils.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*


class TreeMapSerializer<K : Comparable<K>, V>(
    keySerializer: KSerializer<K>, valueSerializer: KSerializer<V>
) : KSerializer<TreeMap<K, V>> {

    private val mapSerializer = MapSerializer(keySerializer, valueSerializer)

    override val descriptor = mapSerializer.descriptor

    override fun serialize(encoder: Encoder, value: TreeMap<K, V>) {
        mapSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): TreeMap<K, V> {
        return TreeMap(mapSerializer.deserialize(decoder))
    }
}