package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import de.tectoast.k18n.generated.K18nLanguage
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.upsert

object GuildLanguageDB : Table("guild_language") {
    val GUILD = long("guild")
    val LANGUAGE = enumerationByName<K18nLanguage>("language", 2)

    override val primaryKey = PrimaryKey(GUILD)
    val cache = SizeLimitedMap<Long, K18nLanguage>(1000)

    suspend fun getLanguage(guild: Long?): K18nLanguage {
        if (guild == null) return K18N_DEFAULT_LANGUAGE
        cache[guild]?.let { return it }
        val lang = dbTransaction {
            select(LANGUAGE).where { GUILD eq guild }.firstOrNull()?.get(LANGUAGE) ?: K18N_DEFAULT_LANGUAGE
        }
        cache[guild] = lang
        return lang
    }

    suspend fun setLanguage(gid: Long, language: K18nLanguage) {
        cache[gid] = language
        dbTransaction {
            upsert {
                it[GUILD] = gid
                it[LANGUAGE] = language
            }
        }
    }
}
