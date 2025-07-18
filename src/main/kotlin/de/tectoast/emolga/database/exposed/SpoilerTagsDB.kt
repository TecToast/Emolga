package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select

object SpoilerTagsDB : Table("spoilertags") {
    val GUILDID = long("guildid")

    override val primaryKey = PrimaryKey(GUILDID)
    /**
     * Checks if the guild has spoiler tags enabled
     * @param guildid the guild id
     * @return true if spoilertags should be used, false otherwise
     */
    suspend fun contains(guildid: Long) = dbTransaction {
        SpoilerTagsDB.select(GUILDID).where { GUILDID eq guildid }.count() > 0
    }

    /**
     * Marks a guild that spoilertags should be used
     * @param guildid the guild id
     */
    suspend fun insert(guildid: Long) = dbTransaction {
        SpoilerTagsDB.insert {
            it[GUILDID] = guildid
        }
    }

    /**
     * Marks a guild that spoilertags shouldn't be used anymore
     * @param guildid the guild id
     * @return if the guild was removed
     */
    suspend fun delete(guildid: Long) = dbTransaction {
        deleteWhere { GUILDID eq guildid } != 0
    }

}
