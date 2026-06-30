package de.tectoast.emolga.utils

import kotlinx.coroutines.flow.Flow

suspend fun <T, K, R> Flow<T>.groupByMapping(keySelector: suspend (T) -> K, mapper: suspend (T) -> R): Map<K, List<R>> {
    val map = mutableMapOf<K, MutableList<R>>()
    collect { item ->
        val key = keySelector(item)
        map.getOrPut(key) { mutableListOf() }.add(mapper(item))
    }
    return map
}