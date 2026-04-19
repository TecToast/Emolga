package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.jsonb
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

object QueuedPicks : Table("queued_picks") {
    val leagueName = varchar("leaguename", 100)
    val idx = integer("idx")
    val data = jsonb<QueuePicksUserData>("data")

    override val primaryKey = PrimaryKey(leagueName, idx)
}

@Single
class QueuedPicksRepository(val db: R2dbcDatabase) {
    suspend fun getForLeague(leagueName: String): Map<Int, QueuePicksUserData> = suspendTransaction(db) {
        QueuedPicks.select(QueuedPicks.idx, QueuedPicks.data).where { (QueuedPicks.leagueName eq leagueName) }
            .toMap { it[QueuedPicks.idx] to it[QueuedPicks.data] }
    }

    suspend fun updateForLeague(leagueName: String, data: Map<Int, QueuePicksUserData>) = suspendTransaction(db) {
        data.forEach { (idx, userData) ->
            QueuedPicks.upsert {
                it[QueuedPicks.leagueName] = leagueName
                it[QueuedPicks.idx] = idx
                it[QueuedPicks.data] = userData
            }
        }
    }

}

@Serializable
sealed interface PickNotification {
    fun wantsNotification(turn: Int): Boolean

    @Serializable
    @SerialName("Always")
    data object Always : PickNotification {
        override fun wantsNotification(turn: Int) = true
    }

    @Serializable
    @SerialName("Specified")
    data class Specified(val turns: Set<Int> = setOf()) : PickNotification {
        override fun wantsNotification(turn: Int) = turns.contains(turn)
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class QueuePicksUserData(
    @EncodeDefault var enabled: Boolean = false,
    var disableIfSniped: Boolean = true,
    @EncodeDefault var queued: MutableList<QueuedAction> = mutableListOf(),
    val pickNotifications: PickNotification? = null
)
