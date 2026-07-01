package de.tectoast.emolga.domain.league.config.repository

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.config.model.LeagueConfigOverride
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreTable
import de.tectoast.emolga.utils.jsonb
import de.tectoast.emolga.utils.newThreadSafeCache
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class LeagueConfigRepository(private val db: R2dbcDatabase) {
    private val cache = newThreadSafeCache<String, LeagueConfig>(10)
    suspend fun getConfig(leagueName: String) = cache.getOrPut(leagueName) {
        getConfigInternal {
            val row = LeagueCoreTable.select(LeagueCoreTable.guild, LeagueCoreTable.configOverride).where {
                LeagueCoreTable.leagueName eq leagueName
            }.first()
            row[LeagueCoreTable.guild] to row[LeagueCoreTable.configOverride]
        }
    }

    suspend fun getLeagueOverride(leagueName: String) = suspendTransaction(db) {
        LeagueCoreTable.select(LeagueCoreTable.configOverride).where { LeagueCoreTable.leagueName eq leagueName }
            .firstOrNull()?.get(LeagueCoreTable.configOverride) ?: LeagueConfigOverride()
    }

    suspend fun getGuildOverride(guildId: Long) = suspendTransaction(db) {
        GuildDefaultConfigTable.select(GuildDefaultConfigTable.config)
            .where { GuildDefaultConfigTable.guildId eq guildId }
            .firstOrNull()?.get(GuildDefaultConfigTable.config) ?: LeagueConfigOverride()
    }

    suspend fun setLeagueOverride(leagueName: String, configOverride: LeagueConfigOverride) =
        suspendTransaction(db) {
            LeagueCoreTable.update({ LeagueCoreTable.leagueName eq leagueName }) {
                it[LeagueCoreTable.configOverride] = configOverride
            }
            cache.remove(leagueName)
        }

    suspend fun setGuildOverride(guildId: Long, configOverride: LeagueConfigOverride) =
        suspendTransaction(db) {
            GuildDefaultConfigTable.upsert {
                it[GuildDefaultConfigTable.guildId] = guildId
                it[GuildDefaultConfigTable.config] = configOverride
            }
            cache.clear()
        }

    suspend inline fun updateGuildOverride(
        guildId: Long,
        update: LeagueConfigOverride.() -> LeagueConfigOverride?
    ): LeagueConfigOverride {
        val current = getGuildOverride(guildId)
        val updated = current.update()
        if (updated != null)
            setGuildOverride(guildId, updated)
        return updated ?: current
    }

    suspend inline fun updateLeagueOverride(
        leagueName: String,
        update: LeagueConfigOverride.() -> LeagueConfigOverride?
    ): LeagueConfigOverride {
        val current = getLeagueOverride(leagueName)
        val updated = current.update()
        if (updated != null)
            setLeagueOverride(leagueName, updated)
        return updated ?: current
    }

    suspend fun fromOverride(guild: Long, configOverride: LeagueConfigOverride?) = getConfigInternal {
        guild to configOverride
    }

    private suspend inline fun getConfigInternal(crossinline supplier: suspend R2dbcTransaction.() -> Pair<Long, LeagueConfigOverride?>) =
        suspendTransaction(db) {
            val (guildId, leagueConfig) = supplier()
            val guildConfig = GuildDefaultConfigTable.select(GuildDefaultConfigTable.config).where {
                GuildDefaultConfigTable.guildId eq guildId
            }.firstOrNull()?.get(GuildDefaultConfigTable.config)
            LeagueConfig() + guildConfig + leagueConfig
        }

    fun clearCache() {
        cache.clear()
    }
}

object GuildDefaultConfigTable : Table("guild_default_config") {
    val guildId = long("guild_id")
    val config = jsonb<LeagueConfigOverride>("config").nullable()

    override val primaryKey = PrimaryKey(guildId)
}
