package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.transactions.transaction

object NaturalGiftDB : IdTable<String>("naturalgift") {
    val NAME = varchar("name", 30)
    val TYPE = varchar("type", 30)
    val BP = integer("bp")

    override val id = NAME.entityId()
}

class NaturalGift(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, NaturalGift>(NaturalGiftDB) {
        fun byName(name: String) = transaction { find { NaturalGiftDB.NAME eq name }.firstOrNull() }
        fun byType(type: String) = transaction { find { NaturalGiftDB.TYPE eq type } }
    }

    var name by NaturalGiftDB.NAME
    var type by NaturalGiftDB.TYPE
    var bp by NaturalGiftDB.BP
}
