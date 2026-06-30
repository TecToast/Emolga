package de.tectoast.emolga.domain.game.repository

import de.tectoast.emolga.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.koin.core.annotation.Single

@Single
class SpoilerTagsRepository(private val db: R2dbcDatabase) {

    /**
     * Checks if the guild has spoiler tags enabled
     * @param guildid the guild id
     * @return true if spoilertags should be used, false otherwise
     */
    suspend fun contains(guildid: Long) = suspendTransaction(db, SpoilerTagsTable) {
        select(this.guildid).where { this.guildid eq guildid }.count() > 0
    }

    /**
     * Marks a guild that spoilertags should be used
     * @param guildid the guild id
     */
    suspend fun insert(guildid: Long) {
        suspendTransaction(db, SpoilerTagsTable) {
            insert {
                it[this.guildid] = guildid
            }
        }
    }

    /**
     * Marks a guild that spoilertags shouldn't be used anymore
     * @param guildid the guild id
     * @return if the guild was removed
     */
    suspend fun delete(guildid: Long) = suspendTransaction(db, SpoilerTagsTable) {
        deleteWhere { this.guildid eq guildid } != 0
    }
}

object SpoilerTagsTable : Table("spoilertags") {
    val guildid = long("guildid")

    override val primaryKey = PrimaryKey(guildid)
}
