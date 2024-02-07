package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object SpoilerTagsDB : Table("spoilertags") {
    val GUILDID = long("guildid")

    /**
     * caches all guilds where spoiler tags should be used in the showdown results
     */

    private val spoilerTags: MutableSet<Long> = HashSet()
    operator fun contains(guildid: Long) = spoilerTags.contains(guildid)

    suspend fun insert(guildid: Long) = newSuspendedTransaction {
        SpoilerTagsDB.insert {
            it[GUILDID] = guildid
        }
        spoilerTags.add(guildid)
    }

    suspend fun delete(guildid: Long) = newSuspendedTransaction {
        spoilerTags.remove(guildid)
        deleteWhere { GUILDID eq guildid } != 0
    }

    suspend fun addToList() = newSuspendedTransaction {
        spoilerTags.addAll(selectAll().map { it[GUILDID] })
    }
}
