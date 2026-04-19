package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.coord.CoordValue
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

typealias SheetTemplateData = List<CoordValue>

object SheetTemplateTable : Table("sheet_template") {
    val templateId = varchar("template_id", 100)
    val pick = jsonb<SheetTemplateData>("pick").nullable()
    val switch = jsonb<SheetTemplateData>("switch").nullable()
    val ban = jsonb<SheetTemplateData>("ban").nullable()


    override val primaryKey = PrimaryKey(templateId)
}

class SheetTemplateRepository(val db: R2dbcDatabase) {
    suspend fun getPickTemplate(templateId: String?): SheetTemplateData? = getTemplate(templateId, SheetTemplateTable.pick)
    suspend fun getSwitchTemplate(templateId: String?): SheetTemplateData? = getTemplate(templateId, SheetTemplateTable.switch)
    suspend fun getBanTemplate(templateId: String?): SheetTemplateData? = getTemplate(templateId, SheetTemplateTable.ban)

    private suspend fun getTemplate(templateId: String?, column: Column<SheetTemplateData?>): SheetTemplateData? {
        if(templateId == null) return null
        return suspendTransaction(db) {
            SheetTemplateTable.select(column)
                .where { SheetTemplateTable.templateId eq templateId }
                .firstOrNull()
                ?.let { it[column] }
        }
    }
}