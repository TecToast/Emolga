package de.tectoast.emolga.domain.guildspecific.sixvspokeworld.repository

import de.tectoast.emolga.domain.guildspecific.sixvspokeworld.model.SixVsPokeworldConfig
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class SixVsPokeworldConfigRepository(private val db: R2dbcDatabase) {
    suspend fun getConfig() = suspendTransaction(db) {
        SixVsPokeworldConfigTable.selectAll().firstOrNull()
            ?.let { SixVsPokeworldConfig(it[SixVsPokeworldConfigTable.challenges]) } ?: SixVsPokeworldConfig()
    }

    suspend fun setConfig(config: SixVsPokeworldConfig) = suspendTransaction(db) {
        SixVsPokeworldConfigTable.deleteAll()
        SixVsPokeworldConfigTable.insert {
            it[challenges] = config.challenges
        }
    }
}

object SixVsPokeworldConfigTable : Table("six_vs_pokeworld_config") {
    val challenges = jsonb<List<SixVsPokeworldConfig.SixVsPokeworldMilestone>>("challenges")
}
