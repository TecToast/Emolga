package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import de.tectoast.k18n.generated.K18nLanguage
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

interface GuildLanguageRepository {
    /**
     * Gets the language for a guild
     * @param guild the guild id
     * @return the language for the guild, or the default language if not set
     */
    suspend fun getLanguage(guild: Long?): K18nLanguage

    /**
     * Sets the language for a guild
     * @param gid the guild id
     * @param language the language to set
     */
    suspend fun setLanguage(gid: Long, language: K18nLanguage)
}

@Single
class PostgresGuildLanguageRepository(val db: R2dbcDatabase, val guildLanguage: GuildLanguageDB) :
    GuildLanguageRepository {
    val cache = SizeLimitedMap<Long, K18nLanguage>(1000)

    override suspend fun getLanguage(guild: Long?): K18nLanguage {
        if (guild == null) return K18N_DEFAULT_LANGUAGE
        cache[guild]?.let { return it }
        val lang = dbTransaction {
            guildLanguage.select(guildLanguage.language).where { guildLanguage.guild eq guild }.firstOrNull()?.get(
                guildLanguage.language
            ) ?: K18N_DEFAULT_LANGUAGE
        }
        cache[guild] = lang
        return lang
    }

    override suspend fun setLanguage(gid: Long, language: K18nLanguage) {
        cache[gid] = language
        dbTransaction {
            guildLanguage.upsert {
                it[this.guild] = gid
                it[this.language] = language
            }
        }
    }
}

@Single
class GuildLanguageDB : Table("guild_language") {
    val guild = long("guild")
    val language = enumerationByName<K18nLanguage>("language", 2)

    override val primaryKey = PrimaryKey(guild)

}
