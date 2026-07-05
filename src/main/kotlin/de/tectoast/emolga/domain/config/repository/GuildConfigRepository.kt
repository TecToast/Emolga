package de.tectoast.emolga.domain.config.repository

import de.tectoast.emolga.domain.config.model.GuildConfigType
import de.tectoast.emolga.utils.newThreadSafeCache
import de.tectoast.emolga.utils.suspendTransaction
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import de.tectoast.k18n.generated.K18nLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.jetbrains.exposed.v1.r2dbc.upsertReturning
import org.koin.core.annotation.Single

@Single
class GuildConfigRepository(private val db: R2dbcDatabase) {
    private val languageCache = newThreadSafeCache<Long, K18nLanguage>(1000)

    suspend fun <T> query(guild: Long, type: GuildConfigType<T>) = suspendTransaction(db, GuildConfigTable) {
        val column = getColumn(type)
        select(column).where { GuildConfigTable.guild eq guild }.firstOrNull()?.get(column) ?: type.default
    }

    suspend fun getLanguage(guild: Long?): K18nLanguage {
        if (guild == null) return K18N_DEFAULT_LANGUAGE
        languageCache[guild]?.let { return it }
        val lang = query(guild, GuildConfigType.Language)
        languageCache[guild] = lang
        return lang
    }

    suspend fun setLanguage(guild: Long, value: K18nLanguage) {
        set(guild, GuildConfigType.Language, value)
        languageCache[guild] = value
    }

    suspend fun <T> set(guild: Long, type: GuildConfigType<T>, value: T) = suspendTransaction(db, GuildConfigTable) {
        val column = getColumn(type)
        upsert {
            it[this.guild] = guild
            it[column] = value
        }
    }

    suspend fun toggle(guild: Long, type: GuildConfigType<Boolean>) = suspendTransaction(db, GuildConfigTable) {
        val column = getColumn(type)
        upsertReturning(onUpdate = {
            it[column] = not(column)
        }) {
            it[this.guild] = guild
            it[column] = !type.default
        }.first()[column]
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getColumn(type: GuildConfigType<T>) : Column<T> = when(type) {
        GuildConfigType.SpoilerTags -> GuildConfigTable.spoilerTags
        GuildConfigType.EnglishResults -> GuildConfigTable.englishResults
        GuildConfigType.EmbedResults -> GuildConfigTable.embedResults
        GuildConfigType.Language -> GuildConfigTable.language
    } as Column<T>
}

object GuildConfigTable : Table() {
    val guild = long("guild")
    val spoilerTags = bool("spoiler_tags").default(GuildConfigType.SpoilerTags.default)
    val englishResults = bool("english_results").default(GuildConfigType.EnglishResults.default)
    val embedResults = bool("embed_results").default(GuildConfigType.EmbedResults.default)
    val language = enumerationByName<K18nLanguage>("language", 32).default(GuildConfigType.Language.default)

    override val primaryKey = PrimaryKey(guild)
}