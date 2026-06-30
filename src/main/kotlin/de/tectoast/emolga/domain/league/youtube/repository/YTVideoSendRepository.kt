package de.tectoast.emolga.domain.league.youtube.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import de.tectoast.emolga.domain.league.youtube.model.YTVideoSaveData
import de.tectoast.emolga.utils.referencesCascade
import kotlinx.coroutines.flow.associate
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class YTVideoSendRepository(private val db: R2dbcDatabase) {
    suspend fun get(
        leagueName: String,
        week: Int,
        battle: Int
    ): YTVideoSaveData = suspendTransaction(db) {
        val raw = YTVideoStateTable.select(YTVideoStateTable.id, YTVideoStateTable.enabled).where {
            (YTVideoStateTable.leagueName eq leagueName) and
                    (YTVideoStateTable.week eq week) and
                    (YTVideoStateTable.battleIndex eq battle)
        }.singleOrNull() ?: return@suspendTransaction YTVideoSaveData(
            enabled = false,
            vids = emptyMap()
        )
        val enabled = raw[YTVideoStateTable.enabled]
        val stateId = raw[YTVideoStateTable.id]
        val vids = YTVideoVidsTable.select(YTVideoVidsTable.videoId, YTVideoVidsTable.idx).where {
            YTVideoVidsTable.stateId eq stateId
        }.associate { it[YTVideoVidsTable.idx] to it[YTVideoVidsTable.videoId] }

        YTVideoSaveData(
            enabled = enabled,
            vids = vids
        )
    }

    suspend fun set(
        leagueName: String,
        week: Int,
        battle: Int,
        idx: Int,
        videoId: String
    ): Boolean {
        return suspendTransaction(db) {
            val raw = YTVideoStateTable.select(YTVideoStateTable.id).where {
                (YTVideoStateTable.leagueName eq leagueName) and
                        (YTVideoStateTable.week eq week) and
                        (YTVideoStateTable.battleIndex eq battle)
            }.singleOrNull() ?: return@suspendTransaction false
            val stateId = raw[YTVideoStateTable.id]
            YTVideoVidsTable.insert {
                it[this.stateId] = stateId
                it[this.idx] = idx
                it[this.videoId] = videoId
            }
            true
        }
    }

    suspend fun enable(leagueName: String, week: Int, battle: Int) {
        suspendTransaction(db) {
            YTVideoStateTable.upsert(
                YTVideoStateTable.leagueName,
                YTVideoStateTable.week,
                YTVideoStateTable.battleIndex
            ) {
                it[this.leagueName] = leagueName
                it[this.week] = week
                it[this.battleIndex] = battle
                it[this.enabled] = true
            }
        }
    }

    suspend fun disable(leagueName: String, week: Int, battle: Int) {
        suspendTransaction(db) {
            YTVideoStateTable.upsert(
                YTVideoStateTable.leagueName,
                YTVideoStateTable.week,
                YTVideoStateTable.battleIndex
            ) {
                it[this.leagueName] = leagueName
                it[this.week] = week
                it[this.battleIndex] = battle
                it[this.enabled] = false
            }
        }
    }

    suspend fun bothVideosPresent(leagueName: String, week: Int, battleIndex: Int) = suspendTransaction(db) {
        val raw = YTVideoStateTable.select(YTVideoStateTable.id).where {
            (YTVideoStateTable.leagueName eq leagueName) and
                    (YTVideoStateTable.week eq week) and
                    (YTVideoStateTable.battleIndex eq battleIndex)
        }.singleOrNull() ?: return@suspendTransaction false
        val stateId = raw[YTVideoStateTable.id]
        val vids = YTVideoVidsTable.selectAll().where { YTVideoVidsTable.stateId eq stateId }.count()
        vids == 2L
    }

}

object YTVideoStateTable : Table("ytvideosend_enable") {
    val id = integer("id").autoIncrement()
    val leagueName = text("league").referencesLeagueName()
    val week = integer("week")
    val battleIndex = integer("battle_index")
    val enabled = bool("enabled")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(leagueName, week, battleIndex)
    }
}

object YTVideoVidsTable : Table("ytvideosend_vids") {
    val stateId = integer("stateid").referencesCascade(YTVideoStateTable.id)
    val idx = integer("idx")
    val videoId = text("videoid")

    override val primaryKey = PrimaryKey(stateId, idx)
}



