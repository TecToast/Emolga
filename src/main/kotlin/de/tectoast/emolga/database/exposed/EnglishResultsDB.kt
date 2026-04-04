package de.tectoast.emolga.database.exposed

import de.tectoast.k18n.generated.K18nLanguage
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class EnglishResultsRepository(
    val db: R2dbcDatabase,
    val guildLanguage: GuildLanguageDB
) {

    /**
     * Checks if the guild has english results enabled
     * @param guild the guild id
     * @return true if english results should be used, false otherwise
     */
    suspend fun contains(guild: Long) = suspendTransaction(db) {
        (EnglishResultsTable.select(EnglishResultsTable.guild).where { EnglishResultsTable.guild eq guild }
            .union(
                guildLanguage.select(guildLanguage.guild)
                    .where { (guildLanguage.guild eq guild) and (guildLanguage.language eq K18nLanguage.EN) })).count() > 0
    }


    /**
     * Marks a guild that english results should be used
     * @param guild the guild id
     */
    suspend fun insert(guild: Long) {
        suspendTransaction(db) {
            EnglishResultsTable.insert {
                it[this.guild] = guild
            }
        }
    }


    /**
     * Marks a guild that english results shouldn't be used anymore
     * @param guild the guild id
     * @return if the guild was removed
     */
    suspend fun delete(guild: Long) = suspendTransaction(db) {
        EnglishResultsTable.deleteWhere { EnglishResultsTable.guild eq guild } != 0
    }
}

object EnglishResultsTable : Table("english_results") {
    val guild = long("guildid")
    override val primaryKey = PrimaryKey(guild)

}
