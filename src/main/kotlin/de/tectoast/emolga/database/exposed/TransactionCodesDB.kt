package de.tectoast.emolga.database.exposed

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface TransactionCodesRepository {
    suspend fun getDataByCode(transactionid: String): Pair<String, Int>?
    suspend fun add(leaguename: String, idx: Int): Uuid
    suspend fun deleteCode(transactionid: String)
}

@OptIn(ExperimentalUuidApi::class)
@Single(binds = [TransactionCodesRepository::class])
class PostgresTransactionCodesRepository(
    private val db: R2dbcDatabase,
    private val table: TransactionCodesDB
) : TransactionCodesRepository {

    override suspend fun getDataByCode(transactionid: String): Pair<String, Int>? {
        val uuid = Uuid.parseHexDashOrNull(transactionid) ?: return null
        return suspendTransaction(db) {
            table.selectAll().where { table.CODE eq uuid }.map { it[table.LEAGUENAME] to it[table.IDX] }.singleOrNull()
        }
    }

    override suspend fun add(leaguename: String, idx: Int): Uuid {
        val code: Uuid = Uuid.generateV7()
        suspendTransaction(db) {
            table.insert {
                it[table.CODE] = code
                it[table.LEAGUENAME] = leaguename
                it[table.IDX] = idx
            }
        }
        return code
    }

    override suspend fun deleteCode(transactionid: String) {
        val uuid = Uuid.parseHexDashOrNull(transactionid) ?: return
        suspendTransaction(db) {
            table.deleteWhere { table.CODE eq uuid }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Single
class TransactionCodesDB : Table("transactioncodes") {
    val CODE = uuid("code")
    val LEAGUENAME = varchar("leaguename", 32)
    val IDX = integer("idx")

    override val primaryKey = PrimaryKey(CODE)

}
