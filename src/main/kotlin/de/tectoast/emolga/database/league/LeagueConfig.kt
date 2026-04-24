package de.tectoast.emolga.database.league

import de.tectoast.emolga.league.config.LeagueConfig
import de.tectoast.emolga.league.config.LeagueConfigOverride
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction


object GuildDefaultConfigTable : Table("guild_default_config") {
    val guildId = long("guild_id")
    val config = jsonb<LeagueConfigOverride>("config").nullable()

    override val primaryKey = PrimaryKey(guildId)
}

class LeagueConfigRepository(val db: R2dbcDatabase) {
    suspend fun getConfig(leagueName: String) = getConfigInternal {
        val row = LeagueCoreTable.select(LeagueCoreTable.guild, LeagueCoreTable.configOverride).where {
            LeagueCoreTable.leagueName eq leagueName
        }.first()
        row[LeagueCoreTable.guild] to row[LeagueCoreTable.configOverride]
    }!!

    suspend fun fromOverride(guild: Long, configOverride: LeagueConfigOverride?) = getConfigInternal {
        guild to configOverride
    }!!

    private suspend inline fun getConfigInternal(crossinline supplier: suspend R2dbcTransaction.() -> Pair<Long, LeagueConfigOverride?>?) =
        suspendTransaction(db) {
            val (guildId, leagueConfig) = supplier() ?: return@suspendTransaction null
            val guildConfig = GuildDefaultConfigTable.select(GuildDefaultConfigTable.config).where {
                GuildDefaultConfigTable.guildId eq guildId
            }.firstOrNull()?.get(GuildDefaultConfigTable.config)
            LeagueConfig() + guildConfig + leagueConfig
        }

}
