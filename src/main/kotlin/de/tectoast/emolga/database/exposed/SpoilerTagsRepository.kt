package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

interface SpoilerTagsRepository {
    /**
     * Checks if the guild has spoiler tags enabled
     * @param guildid the guild id
     * @return true if spoilertags should be used, false otherwise
     */
    suspend fun contains(guildid: Long): Boolean

    /**
     * Marks a guild that spoilertags should be used
     * @param guildid the guild id
     */
    suspend fun insert(guildid: Long)

    /**
     * Marks a guild that spoilertags shouldn't be used anymore
     * @param guildid the guild id
     * @return if the guild was removed
     */
    suspend fun delete(guildid: Long): Boolean
}

@Single
class PostgresSpoilerTagsRepository(val db: R2dbcDatabase, val spoilerTags: SpoilerTagsDB) : SpoilerTagsRepository {

    override suspend fun contains(guildid: Long) = suspendTransaction(db) {
        spoilerTags.select(spoilerTags.guildid).where { spoilerTags.guildid eq guildid }.count() > 0
    }

    override suspend fun insert(guildid: Long) {
        suspendTransaction(db) {
            spoilerTags.insert {
                it[this.guildid] = guildid
            }
        }
    }

    override suspend fun delete(guildid: Long) = suspendTransaction(db) {
        spoilerTags.deleteWhere { spoilerTags.guildid eq guildid } != 0
    }
}

@Single
class SpoilerTagsDB : Table("spoilertags") {
    val guildid = long("guildid")

    override val primaryKey = PrimaryKey(guildid)
}
