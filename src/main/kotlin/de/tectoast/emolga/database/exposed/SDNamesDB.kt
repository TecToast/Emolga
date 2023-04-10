package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.Command
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object SDNamesDB : Table("sdnames") {
    val NAME = varchar("name", 18)
    val ID = long("id")

    fun getIDByName(name: String) =
        transaction { select { NAME eq Command.toUsername(name) }.firstOrNull()?.get(ID) } ?: -1

    fun addIfAbsent(name: String, id: Long) = transaction {
        (select { NAME eq name }.firstOrNull() == null).also { b ->
            if (b)
                insert {
                    it[NAME] = name
                    it[ID] = id
                }
        }
    }
}
