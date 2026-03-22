package de.tectoast.emolga.utils

import de.tectoast.emolga.database.exposed.toMap
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.json.jsonb
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Singleton

interface ReplayDataStoreRepository {
    suspend fun set(leagueName: String, data: FullGameData)

    suspend fun get(leagueName: String, gameday: Int, matchNum: Int): FullGameData?

    suspend fun getAll(leagueName: String): List<FullGameData>

    suspend fun getByGameday(leagueName: String, gameday: Int): Map<Int, FullGameData>

    suspend fun getByIdx(leagueName: String, gameday: Int, idx: Int): FullGameData?

}

object ReplayDataStoreDB : Table("replaydatastore") {
    val LEAGUE = text("league")
    val GAMEDAY = integer("gameday")
    val BATTLEINDEX = integer("battleindex")
    val UINDICES = array<Int>("uindices")
    val DATA = jsonb<FullGameData>("data", myJSON)

    override val primaryKey = PrimaryKey(LEAGUE, GAMEDAY, BATTLEINDEX)
}

@Singleton
class ExposedReplayDataStoreRepository(val db: R2dbcDatabase) : ReplayDataStoreRepository {
    override suspend fun set(leagueName: String, data: FullGameData) {
        suspendTransaction(db) {
            ReplayDataStoreDB.upsert {
                it[LEAGUE] = leagueName
                it[GAMEDAY] = data.gameday
                it[BATTLEINDEX] = data.battleIndex
                it[UINDICES] = data.uindices
                it[DATA] = data
            }
        }
    }

    override suspend fun get(
        leagueName: String,
        gameday: Int,
        matchNum: Int
    ): FullGameData? = suspendTransaction(db) {
        ReplayDataStoreDB.select(ReplayDataStoreDB.DATA).where {
            (ReplayDataStoreDB.LEAGUE eq leagueName) and
                    (ReplayDataStoreDB.GAMEDAY eq gameday) and
                    (ReplayDataStoreDB.BATTLEINDEX eq matchNum)
        }.map { it[ReplayDataStoreDB.DATA] }.firstOrNull()
    }

    override suspend fun getAll(leagueName: String): List<FullGameData> = suspendTransaction(db) {
        ReplayDataStoreDB.select(ReplayDataStoreDB.DATA).where {
            (ReplayDataStoreDB.LEAGUE eq leagueName)
        }.map { it[ReplayDataStoreDB.DATA] }.toList()
    }

    override suspend fun getByGameday(
        leagueName: String,
        gameday: Int
    ): Map<Int, FullGameData> {
        return suspendTransaction(db) {
            ReplayDataStoreDB.select(ReplayDataStoreDB.DATA, ReplayDataStoreDB.BATTLEINDEX).where {
                (ReplayDataStoreDB.LEAGUE eq leagueName) and
                        (ReplayDataStoreDB.GAMEDAY eq gameday)
            }.toMap { it[ReplayDataStoreDB.BATTLEINDEX] to it[ReplayDataStoreDB.DATA] }
        }
    }

    override suspend fun getByIdx(
        leagueName: String,
        gameday: Int,
        idx: Int
    ): FullGameData? {
        return suspendTransaction(db) {
            ReplayDataStoreDB.select(ReplayDataStoreDB.DATA).where {
                (ReplayDataStoreDB.LEAGUE eq leagueName) and
                        (ReplayDataStoreDB.GAMEDAY eq gameday) and
                        (intLiteral(idx) eq anyFrom(ReplayDataStoreDB.UINDICES))
            }.map { it[ReplayDataStoreDB.DATA] }.singleOrNull()
        }
    }
}