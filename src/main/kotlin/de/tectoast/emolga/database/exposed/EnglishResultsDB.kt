package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.k18n.generated.K18nLanguage
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.union

object EnglishResultsDB : Table("english_results") {
    val GUILDID = long("guildid")
    override val primaryKey = PrimaryKey(GUILDID)

    /**
     * Checks if the guild has english results enabled
     * @param guildid the guild id
     * @return true if english results should be used, false otherwise
     */
    suspend fun contains(guildid: Long) = dbTransaction {
        (select(GUILDID).where { GUILDID eq guildid }
            .union(
                GuildLanguageDB.select(GuildLanguageDB.GUILD)
                    .where { (GuildLanguageDB.GUILD eq guildid) and (GuildLanguageDB.LANGUAGE eq K18nLanguage.EN) })).count() > 0
    }

    /**
     * Marks a guild that english results should be used
     * @param guildid the guild id
     */
    suspend fun insert(guildid: Long) = dbTransaction {
        insert {
            it[GUILDID] = guildid
        }
    }

    /**
     * Marks a guild that english results shouldn't be used anymore
     * @param guildid the guild id
     * @return if the guild was removed
     */
    suspend fun delete(guildid: Long) = dbTransaction {
        deleteWhere { GUILDID eq guildid } != 0
    }
}
