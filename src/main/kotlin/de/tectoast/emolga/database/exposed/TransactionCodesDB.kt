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
@Single
class TransactionCodesRepository(
    private val db: R2dbcDatabase,
) {

    suspend fun getDataByCode(transactionid: String): Pair<String, Int>? {
        val uuid = Uuid.parseHexDashOrNull(transactionid) ?: return null
        return suspendTransaction(db) {
            TransactionCodesTable.selectAll().where { TransactionCodesTable.CODE eq uuid }
                .map { it[TransactionCodesTable.LEAGUENAME] to it[TransactionCodesTable.IDX] }
                .singleOrNull()
        }
    }

    suspend fun add(leaguename: String, idx: Int): Uuid {
        val code: Uuid = Uuid.generateV7()
        suspendTransaction(db) {
            TransactionCodesTable.insert {
                it[TransactionCodesTable.CODE] = code
                it[TransactionCodesTable.LEAGUENAME] = leaguename
                it[TransactionCodesTable.IDX] = idx
            }
        }
        return code
    }

    suspend fun deleteCode(transactionid: String) {
        val uuid = Uuid.parseHexDashOrNull(transactionid) ?: return
        suspendTransaction(db) {
            TransactionCodesTable.deleteWhere { TransactionCodesTable.CODE eq uuid }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
object TransactionCodesTable : Table("transactioncodes") {
    val CODE = uuid("code")
    val LEAGUENAME = varchar("leaguename", 32)
    val IDX = integer("idx")

    override val primaryKey = PrimaryKey(CODE)

}
