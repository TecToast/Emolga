package de.tectoast.emolga.domain.ytgeneric.repository

import de.tectoast.emolga.domain.ytgeneric.model.YTNotificationData
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class YouTubeNotificationsRepository(private val db: R2dbcDatabase) {

    suspend fun getAllYTChannels() = suspendTransaction(db) {
        YTNotificationsTable.select(YTNotificationsTable.ytChannel).withDistinct(true)
            .map { it[YTNotificationsTable.ytChannel] }
            .toSet()
    }

    suspend fun getDCChannels(ytChannel: String) = suspendTransaction(db) {
        YTNotificationsTable.select(YTNotificationsTable.discordChannel, YTNotificationsTable.dm)
            .where { YTNotificationsTable.ytChannel eq ytChannel }
            .map {
                YTNotificationData(
                    it[YTNotificationsTable.discordChannel],
                    it[YTNotificationsTable.dm],
                    it[YTNotificationsTable.format]
                )
            }.toList()
    }
}

object YTNotificationsTable : Table("ytnotifications") {
    val discordChannel = long("dcchannel")
    val ytChannel = text("ytchannel")
    val dm = bool("dm").default(false)
    val format = text("format").default("{ytlink}")

    override val primaryKey = PrimaryKey(discordChannel, ytChannel)

}
