package de.tectoast.emolga.domain.statistics.repository

import de.tectoast.emolga.domain.statistics.model.FeatureType
import de.tectoast.emolga.utils.jsonb
import de.tectoast.emolga.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Instant

@Single
class FeatureStatsRepository(@Named("stats") private val db: R2dbcDatabase) {
    suspend fun addInvocation(
        featureName: String,
        type: FeatureType,
        args: Map<String, String>,
        timestamp: Instant,
        user: Long,
        guild: Long?,
        channel: Long?
    ) =
        suspendTransaction(db, FeatureStatsTable) {
            insert {
                it[this.user] = user
                it[this.featureName] = featureName
                it[this.type] = type
                it[this.args] = args
                it[this.timestamp] = timestamp
                it[this.guild] = guild
                it[this.channel] = channel
            }
        }
}

object FeatureStatsTable : Table("feature_stats") {
    val id = integer("id").autoIncrement()
    val user = long("user")
    val featureName = text("feature")
    val type = enumerationByName<FeatureType>("type", 64)
    val args = jsonb<Map<String, String>>("args")
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
    val guild = long("guild").nullable()
    val channel = long("channel").nullable()

    override val primaryKey = PrimaryKey(id)
}