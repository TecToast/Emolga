package de.tectoast.emolga.domain.league.draft.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single


@Single
class DraftLastAnnounceRepository(private val db: R2dbcDatabase) {
    suspend fun getLastAnnounceId(leagueName: String, session: Int) = suspendTransaction(db) {
        DraftLastAnnounceIdTable.select(DraftLastAnnounceIdTable.lastAnnounceId)
            .where { (DraftLastAnnounceIdTable.leagueName eq leagueName) and (DraftLastAnnounceIdTable.session eq session) }
            .firstOrNull()?.get(DraftLastAnnounceIdTable.lastAnnounceId)
    }

    suspend fun setLastAnnounceId(leagueName: String, session: Int, id: Long) = suspendTransaction(db) {
        DraftLastAnnounceIdTable.upsert {
            it[DraftLastAnnounceIdTable.leagueName] = leagueName
            it[DraftLastAnnounceIdTable.session] = session
            it[DraftLastAnnounceIdTable.lastAnnounceId] = id
        }
    }
}

object DraftLastAnnounceIdTable : Table("draft_last_announce_id") {
    val leagueName = text("league_name").referencesLeagueName()
    val session = integer("session")
    val lastAnnounceId = long("last_announce_id")

    override val primaryKey = PrimaryKey(leagueName, session)
}
