package de.tectoast.emolga.domain.pokemon.model

import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.currentDialect

class ShowdownIdColumnType : ColumnType<ShowdownID>() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): ShowdownID = when (value) {
        is String -> ShowdownID(value)
        else -> error("Unexpected value of type Int: $value of ${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: ShowdownID): Any {
        return value.value
    }
}

fun Table.showdownIDColumn(name: String = "showdown_id") = registerColumn(name, ShowdownIdColumnType())