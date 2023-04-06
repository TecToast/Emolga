package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.Command
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object SpoilerTagsDB : Table("spoilertags") {
    val GUILDID = long("guildid")

    fun insert(guildid: Long) = transaction {
        SpoilerTagsDB.insert {
            it[GUILDID] = guildid
        }
    }

    fun delete(guildid: Long) = transaction {
        deleteWhere { GUILDID eq guildid } != 0
    }

    fun addToList() = transaction {
        Command.spoilerTags.addAll(selectAll().map { it[GUILDID] })
    }
}
