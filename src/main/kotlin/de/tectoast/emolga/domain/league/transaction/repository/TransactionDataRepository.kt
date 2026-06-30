package de.tectoast.emolga.domain.league.transaction.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import de.tectoast.emolga.domain.league.transaction.model.TransactionAmounts
import de.tectoast.emolga.domain.league.transaction.model.TransactionEntry
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.database.ShowdownIdColumnType
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.associate
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single


@Single
class TransactionDataRepository(private val db: R2dbcDatabase) {
    suspend fun getTransactionAmounts(leagueName: String, idx: Int) = suspendTransaction(db, TransactionAmountsTable) {
        select(mons, extraTeras).where { (this.leagueName eq leagueName) and (this.idx eq idx) }.firstOrNull()?.let {
            TransactionAmounts(it[mons], it[extraTeras])
        } ?: TransactionAmounts()
    }

    suspend fun setTransactionAmounts(leagueName: String, idx: Int, amounts: TransactionAmounts) =
        suspendTransaction(db, TransactionAmountsTable) {
            upsert {
                it[this.leagueName] = leagueName
                it[this.idx] = idx
                it[mons] = amounts.mons
                it[extraTeras] = amounts.extraTeras
            }
        }

    suspend fun setRunning(leagueName: String, week: Int, idx: Int, drops: List<ShowdownID>, picks: List<ShowdownID>) =
        suspendTransaction(db, TransactionRunningTable) {
            upsert {
                it[this.leagueName] = leagueName
                it[this.week] = week
                it[this.idx] = idx
                it[this.drops] = drops
                it[this.picks] = picks
            }
        }

    suspend fun getRunning(leagueName: String, week: Int) = suspendTransaction(db, TransactionRunningTable) {
        select(idx, drops, picks).where { (this.leagueName eq leagueName) and (this.week eq week) }
            .associate {
                it[idx] to TransactionEntry(
                    it[drops].toMutableList(),
                    it[picks].toMutableList()
                )
            }
    }

    suspend fun removeRunning(leagueName: String, week: Int, indices: Iterable<Int>) =
        suspendTransaction(db, TransactionRunningTable) {
            deleteWhere { (this.leagueName eq leagueName) and (this.week eq week) and (this.idx inList indices) }
        }
}


object TransactionRunningTable : Table("transaction_data") {
    val leagueName = text("league_name").referencesLeagueName()
    val week = integer("week")
    val idx = integer("idx")
    val drops = array("drops", ShowdownIdColumnType()).default(emptyList())
    val picks = array("picks", ShowdownIdColumnType()).default(emptyList())

    override val primaryKey = PrimaryKey(leagueName, week, idx)
}

object TransactionAmountsTable : Table("transaction_amounts") {
    val leagueName = text("league_name").referencesLeagueName()
    val idx = integer("idx")
    val mons = integer("mons").default(0)
    val extraTeras = integer("extra_teras").default(0)

    override val primaryKey = PrimaryKey(leagueName, idx)
}
