package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table

object DumbestFliesDB : Table("dumbestflies") {
    val id = long("id")
    val name = varchar("name", 32)
    override val primaryKey = PrimaryKey(id)
}
