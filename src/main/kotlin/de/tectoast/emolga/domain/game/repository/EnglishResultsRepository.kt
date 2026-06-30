package de.tectoast.emolga.domain.game.repository

import de.tectoast.emolga.domain.language.repository.GuildLanguageTable
import de.tectoast.k18n.generated.K18nLanguage
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class EnglishResultsRepository(
    private val db: R2dbcDatabase
) {

    /**
     * Checks if the guild has English results enabled
     * @param guild the guild id
     * @return true if English results should be used, false otherwise
     */
    suspend fun contains(guild: Long) = suspendTransaction(db) {
        (EnglishResultsTable.select(EnglishResultsTable.guild).where { EnglishResultsTable.guild eq guild }
            .union(
                GuildLanguageTable.select(GuildLanguageTable.guild)
                    .where { (GuildLanguageTable.guild eq guild) and (GuildLanguageTable.language eq K18nLanguage.EN) })).count() > 0
    }


    /**
     * Marks a guild that English results should be used
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
     * Marks a guild that English results shouldn't be used anymore
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
