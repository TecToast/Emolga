package de.tectoast.emolga.utils

import java.util.*

class SizeLimitedMap<K, V>(private val maxSize: Int = 5) : LinkedHashMap<K, V>() {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > maxSize
    }
}

fun <K, V> newThreadSafeCache(maxSize: Int = 5): MutableMap<K, V> {
    return Collections.synchronizedMap(SizeLimitedMap(maxSize))
}