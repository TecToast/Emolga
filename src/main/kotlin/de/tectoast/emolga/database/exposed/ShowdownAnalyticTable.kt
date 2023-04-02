package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.httpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

abstract class ShowdownAnalyticTable(name: String) : IdTable<String>(name) {
    override val id = varchar("id", 65).entityId()
    override val primaryKey = PrimaryKey(id)

    suspend fun insertReplays(urls: List<String>) {
        val ids = urls.associateBy { it.substringAfterLast("/") }.toMutableMap()
        val existing = newSuspendedTransaction {
            select { this@ShowdownAnalyticTable.id inList ids.keys }.map { it[this@ShowdownAnalyticTable.id].value }
                .toSet()
        }
        ids.keys.removeAll(existing)
        ids.values.asFlow()
            .map { delay(2000); it to httpClient.get("$it.log").bodyAsText().split("\n"); }
            .collect {
                insertSingleReplay(it.first.substringAfterLast("/"), it.second)
            }
    }

    abstract suspend fun insertSingleReplay(replayId: String, lines: List<String>, extraData: String? = null)
    abstract suspend fun allStats(): String
}
