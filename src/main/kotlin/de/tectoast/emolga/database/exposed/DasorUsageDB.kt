package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.embedColor
import dev.minn.jda.ktx.messages.Embed
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object DasorUsageDB : Table("dasorusage") {
    val POKEMON = varchar("pokemon", 30)
    val USES = integer("uses")

    fun buildMessage() = transaction {
        Embed {
            title = "Dasor Statistik mit coolen Mons"
            description =
                selectAll().orderBy(USES to SortOrder.DESC).joinToString("\n") { "${it[POKEMON]}: ${it[USES]}" }
            color = embedColor
        }
    }
}
