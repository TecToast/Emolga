package de.tectoast.emolga.domain.language.repository

import de.tectoast.emolga.utils.newThreadSafeCache
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import de.tectoast.k18n.generated.K18nLanguage
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

@Single
class GuildLanguageRepository(private val db: R2dbcDatabase) {
    private val cache = newThreadSafeCache<Long, K18nLanguage>(1000)

    /**
     * Gets the language for a guild
     * @param guild the guild id
     * @return the language for the guild, or the default language if not set
     */
    suspend fun getLanguage(guild: Long?): K18nLanguage {
        if (guild == null) return K18N_DEFAULT_LANGUAGE
        cache[guild]?.let { return it }
        val lang = suspendTransaction {
            GuildLanguageTable.select(GuildLanguageTable.language).where { GuildLanguageTable.guild eq guild }
                .firstOrNull()?.get(
                    GuildLanguageTable.language
                ) ?: K18N_DEFAULT_LANGUAGE
        }
        cache[guild] = lang
        return lang
    }

    /**
     * Sets the language for a guild
     * @param gid the guild id
     * @param language the language to set
     */
    suspend fun setLanguage(gid: Long, language: K18nLanguage) {
        cache[gid] = language
        suspendTransaction {
            GuildLanguageTable.upsert {
                it[guild] = gid
                it[this.language] = language
            }
        }
    }
}

object GuildLanguageTable : Table("guild_language") {
    val guild = long("guild")
    val language = enumerationByName<K18nLanguage>("language", 2)

    override val primaryKey = PrimaryKey(guild)

}
