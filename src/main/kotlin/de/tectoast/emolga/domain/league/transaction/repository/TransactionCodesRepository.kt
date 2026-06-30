package de.tectoast.emolga.domain.league.transaction.repository

import de.tectoast.emolga.di.CleanupTask
import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Single
class TransactionCodesRepository(
    private val db: R2dbcDatabase,
    private val clock: Clock
) : CleanupTask {

    override suspend fun cleanup(now: Instant) {
        suspendTransaction(db) {
            TransactionCodesTable.deleteWhere { TransactionCodesTable.timestamp lessEq now - 1.days }
        }
    }

    suspend fun getDataByCode(transactionid: Uuid): Pair<String, Int>? {
        return suspendTransaction(db) {
            TransactionCodesTable.selectAll().where { TransactionCodesTable.code eq transactionid }
                .map { it[TransactionCodesTable.leagueName] to it[TransactionCodesTable.idx] }
                .singleOrNull()
        }
    }

    suspend fun add(leaguename: String, idx: Int): Uuid {
        val code: Uuid = Uuid.generateV7()
        suspendTransaction(db) {
            TransactionCodesTable.insert {
                it[TransactionCodesTable.code] = code
                it[TransactionCodesTable.leagueName] = leaguename
                it[TransactionCodesTable.idx] = idx
                it[TransactionCodesTable.timestamp] = clock.now()
            }
        }
        return code
    }

    suspend fun deleteCode(transactionid: Uuid) {
        suspendTransaction(db) {
            TransactionCodesTable.deleteWhere { TransactionCodesTable.code eq transactionid }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
object TransactionCodesTable : Table("transactioncodes") {
    val code = uuid("code")
    val leagueName = text("leaguename").referencesLeagueName()
    val idx = integer("idx")
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(code)

}
