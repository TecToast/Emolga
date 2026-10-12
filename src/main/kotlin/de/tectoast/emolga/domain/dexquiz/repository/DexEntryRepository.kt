package de.tectoast.emolga.domain.dexquiz.repository

import de.tectoast.emolga.domain.dexquiz.model.DexEntry
import de.tectoast.emolga.domain.pokemon.model.showdownIDColumn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.Random
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class DexEntryRepository(private val db: R2dbcDatabase) {
    suspend fun getDexEntryById(id: Int): DexEntry? = suspendTransaction(db) {
        DexEntryTable.selectAll().where { DexEntryTable.id eq id }
            .map { it.rowToData() }
            .firstOrNull()
    }

    suspend fun getRandomDexEntry(): DexEntry? = suspendTransaction(db) {
        DexEntryTable.selectAll()
            .orderBy(Random())
            .limit(1)
            .map { it.rowToData() }
            .firstOrNull()
    }

    private fun ResultRow.rowToData(): DexEntry = with(DexEntryTable) {
        DexEntry(get(id), get(showdownId), get(games), get(entry))
    }
}

object DexEntryTable : Table("dex_entries") {
    val id = integer("id").autoIncrement()
    val showdownId = showdownIDColumn()
    val games = array<String>("games")
    val entry = text("entry")
}