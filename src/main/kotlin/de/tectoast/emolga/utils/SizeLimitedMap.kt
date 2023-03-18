package de.tectoast.emolga.utils

class SizeLimitedMap<K, V>(private val maxSize: Int = 5) : LinkedHashMap<K, V>() {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > maxSize
    }
}
