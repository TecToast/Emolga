package de.tectoast.emolga.domain.game.repository

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class EmbedResultsRepository(private val db: R2dbcDatabase) {
    suspend fun hasEmbedResults(guild: Long) = suspendTransaction(db) {
        DisabledEmbedResultsTable.selectAll().where { DisabledEmbedResultsTable.guild eq guild }.count() == 0L
    }

    suspend fun disableEmbedResults(guild: Long) = suspendTransaction(db) {
        DisabledEmbedResultsTable.insert { it[DisabledEmbedResultsTable.guild] = guild }
    }

    suspend fun enableEmbedResults(guild: Long) = suspendTransaction(db) {
        DisabledEmbedResultsTable.deleteWhere { DisabledEmbedResultsTable.guild eq guild } != 0
    }
}

object DisabledEmbedResultsTable : Table("embed_results_disabled") {
    val guild = long("guild")

    override val primaryKey = PrimaryKey(guild)
}