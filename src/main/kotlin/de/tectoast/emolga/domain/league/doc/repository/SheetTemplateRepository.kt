package de.tectoast.emolga.domain.league.doc.repository

import de.tectoast.emolga.domain.league.doc.model.SheetTemplateData
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class SheetTemplateRepository(private val db: R2dbcDatabase) {
    suspend fun getPickTemplate(templateId: String?): SheetTemplateData? =
        getTemplate(templateId, SheetTemplateTable.pick)

    suspend fun getSwitchTemplate(templateId: String?): SheetTemplateData? =
        getTemplate(templateId, SheetTemplateTable.switch)

    suspend fun getBanTemplate(templateId: String?): SheetTemplateData? =
        getTemplate(templateId, SheetTemplateTable.ban)

    private suspend fun getTemplate(templateId: String?, column: Column<SheetTemplateData?>): SheetTemplateData? {
        if (templateId == null) return null
        return suspendTransaction(db) {
            SheetTemplateTable.select(column)
                .where { SheetTemplateTable.templateId eq templateId }
                .firstOrNull()
                ?.let { it[column] }
        }
    }
}

object SheetTemplateTable : Table("sheet_template") {
    val templateId = text("template_id")
    val pick = jsonb<SheetTemplateData>("pick").nullable()
    val switch = jsonb<SheetTemplateData>("switch").nullable()
    val ban = jsonb<SheetTemplateData>("ban").nullable()


    override val primaryKey = PrimaryKey(templateId)
}