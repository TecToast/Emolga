package de.tectoast.emolga.domain.guildspecific.nominate.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single


@Single
class NominateRepository(private val db: R2dbcDatabase) {
    suspend fun setNominations(league: String, week: Int, idx: Int, nominations: List<Int>) = suspendTransaction(db) {
        NominateTable.upsert {
            it[NominateTable.league] = league
            it[NominateTable.week] = week
            it[NominateTable.idx] = idx
            it[NominateTable.nominations] = nominations
        }
    }

    suspend fun hasNominated(league: String, week: Int, idx: Int) = suspendTransaction(db) {
        NominateTable.selectAll()
            .where { (NominateTable.league eq league) and (NominateTable.week eq week) and (NominateTable.idx eq idx) }
            .count() > 0
    }
}

object NominateTable : Table("nominate") {
    val league = text("league").referencesLeagueName()
    val idx = integer("idx")
    val week = integer("week")
    val nominations = array<Int>("nominations")

    override val primaryKey = PrimaryKey(league, week, idx)
}