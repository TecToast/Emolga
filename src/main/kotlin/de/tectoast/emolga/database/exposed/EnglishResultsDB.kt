package de.tectoast.emolga.database.exposed

import de.tectoast.k18n.generated.K18nLanguage
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

interface EnglishResultsRepository {
    /**
     * Marks a guild that english results should be used
     * @param guild the guild id
     */
    suspend fun insert(guild: Long)
    /**
     * Checks if the guild has english results enabled
     * @param guild the guild id
     * @return true if english results should be used, false otherwise
     */
    suspend fun contains(guild: Long): Boolean
    /**
     * Marks a guild that english results shouldn't be used anymore
     * @param guild the guild id
     * @return if the guild was removed
     */
    suspend fun delete(guild: Long): Boolean
}

@Single(binds = [EnglishResultsRepository::class])
class PostgresEnglishResultsRepository(
    val db: R2dbcDatabase,
    val englishResults: EnglishResultsDB,
    val guildLanguage: GuildLanguageDB
) : EnglishResultsRepository {

    override suspend fun contains(guild: Long) = suspendTransaction(db) {
        (englishResults.select(englishResults.guild).where { englishResults.guild eq guild }
            .union(
                guildLanguage.select(guildLanguage.guild)
                    .where { (guildLanguage.guild eq guild) and (guildLanguage.language eq K18nLanguage.EN) })).count() > 0
    }


    override suspend fun insert(guild: Long) {
        suspendTransaction(db) {
            englishResults.insert {
                it[this.guild] = guild
            }
        }
    }


    override suspend fun delete(guild: Long) = suspendTransaction(db) {
        englishResults.deleteWhere { englishResults.guild eq guild } != 0
    }
}

@Single
class EnglishResultsDB : Table("english_results") {
    val guild = long("guildid")
    override val primaryKey = PrimaryKey(guild)

}
