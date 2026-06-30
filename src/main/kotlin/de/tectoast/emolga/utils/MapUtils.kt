package de.tectoast.emolga.utils

fun <K> MutableMap<K, Int>.add(key: K, value: Int) = compute(key) { _, v ->
    v?.plus(value) ?: value
}

fun MutableMap<String, Int>.addFromMutable(other: Map<String, Int>) {
    for ((key, value) in other) {
        this.add(key, value)
    }
}

fun Map<String, Int>.addFrom(other: Map<String, Int>): Map<String, Int> {
    val result = this.toMutableMap()
    result.addFromMutable(other)
    return result
}

fun Map<String, Int>.subtractFrom(other: Map<String, Int>): Map<String, Int> {
    val result = this.toMutableMap()
    for ((key, value) in other) {
        result.add(key, -value)
    }
    return result
}
