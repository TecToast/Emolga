package de.tectoast.emolga.utils

import de.tectoast.emolga.database.exposed.toMap
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

interface YTVideoSendRepository {
    suspend fun get(leagueName: String, gameday: Int, battle: Int): YTVideoSaveData

    suspend fun set(leagueName: String, gameday: Int, battle: Int, idx: Int, videoId: String): Boolean

    suspend fun enable(leagueName: String, gameday: Int, battle: Int)

    suspend fun isEnabled(leagueName: String, gameday: Int, battle: Int): Boolean

    suspend fun disable(leagueName: String, gameday: Int, battle: Int)

}

object YTVideoStateDB : Table("ytvideosend_enable") {
    val ID = integer("id").autoIncrement()
    val LEAGUE = text("league")
    val GAMEDAY = integer("gameday")
    val BATTLEINDEX = integer("battleindex")
    val ENABLED = bool("enabled")

    override val primaryKey = PrimaryKey(ID)

    init {
        uniqueIndex(LEAGUE, GAMEDAY, BATTLEINDEX)
    }
}

object YTVideoVidsDB : Table("ytvideosend_vids") {
    val STATEID = integer("stateid").references(YTVideoStateDB.ID, onDelete = ReferenceOption.CASCADE)
    val IDX = integer("idx")
    val VIDEOID = varchar("videoid", 32)

    override val primaryKey = PrimaryKey(STATEID, IDX)

}

@Single
class ExposedYTVideoSendRepository(val db: R2dbcDatabase) : YTVideoSendRepository {
    override suspend fun get(
        leagueName: String,
        gameday: Int,
        battle: Int
    ): YTVideoSaveData = suspendTransaction(db) {
        val raw = YTVideoStateDB.select(YTVideoStateDB.ID, YTVideoStateDB.ENABLED).where {
            (YTVideoStateDB.LEAGUE eq leagueName) and
                    (YTVideoStateDB.GAMEDAY eq gameday) and
                    (YTVideoStateDB.BATTLEINDEX eq battle)
        }.singleOrNull() ?: return@suspendTransaction YTVideoSaveData(
            enabled = false,
            vids = emptyMap()
        )
        val enabled = raw[YTVideoStateDB.ENABLED]
        val stateId = raw[YTVideoStateDB.ID]
        val vids = YTVideoVidsDB.select(YTVideoVidsDB.VIDEOID, YTVideoVidsDB.IDX).where {
            YTVideoVidsDB.STATEID eq stateId
        }.toMap { it[YTVideoVidsDB.IDX] to it[YTVideoVidsDB.VIDEOID] }

        YTVideoSaveData(
            enabled = enabled,
            vids = vids
        )
    }

    override suspend fun set(
        leagueName: String,
        gameday: Int,
        battle: Int,
        idx: Int,
        videoId: String
    ): Boolean {
        return suspendTransaction(db) {
            val raw = YTVideoStateDB.select(YTVideoStateDB.ID).where {
                (YTVideoStateDB.LEAGUE eq leagueName) and
                        (YTVideoStateDB.GAMEDAY eq gameday) and
                        (YTVideoStateDB.BATTLEINDEX eq battle)
            }.singleOrNull() ?: return@suspendTransaction false
            val stateId = raw[YTVideoStateDB.ID]
            YTVideoVidsDB.insert {
                it[STATEID] = stateId
                it[IDX] = idx
                it[VIDEOID] = videoId
            }
            true
        }
    }

    override suspend fun enable(leagueName: String, gameday: Int, battle: Int) {
        suspendTransaction(db) {
            YTVideoStateDB.upsert(YTVideoStateDB.LEAGUE, YTVideoStateDB.GAMEDAY, YTVideoStateDB.BATTLEINDEX) {
                it[LEAGUE] = leagueName
                it[GAMEDAY] = gameday
                it[BATTLEINDEX] = battle
                it[ENABLED] = true
            }
        }
    }

    override suspend fun isEnabled(
        leagueName: String,
        gameday: Int,
        battle: Int
    ): Boolean {
        return suspendTransaction(db) {
            YTVideoStateDB.select(YTVideoStateDB.ENABLED).where {
                (YTVideoStateDB.LEAGUE eq leagueName) and
                        (YTVideoStateDB.GAMEDAY eq gameday) and
                        (YTVideoStateDB.BATTLEINDEX eq battle)
            }.map { it[YTVideoStateDB.ENABLED] }.singleOrNull() ?: false
        }
    }

    override suspend fun disable(leagueName: String, gameday: Int, battle: Int) {
        suspendTransaction(db) {
            YTVideoStateDB.upsert(YTVideoStateDB.LEAGUE, YTVideoStateDB.GAMEDAY, YTVideoStateDB.BATTLEINDEX) {
                it[LEAGUE] = leagueName
                it[GAMEDAY] = gameday
                it[BATTLEINDEX] = battle
                it[ENABLED] = false
            }
        }
    }
}