package de.tectoast.emolga.utils.json.emolga

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import java.time.Instant

@Serializable
data class Statistics(
    @Contextual val timestamp: Instant,
    val meta: String,
    val count: Int
)

suspend fun CoroutineCollection<Statistics>.increment(meta: String) = withContext(Dispatchers.IO) {
    val count = getCount(meta)
    insertOne(Statistics(Instant.now(), meta, count + 1))
}

suspend fun CoroutineCollection<Statistics>.getCount(meta: String) = withContext(Dispatchers.IO) {
    find(Statistics::meta eq meta).descendingSort(Statistics::timestamp).limit(1).first()?.count ?: 0
}
