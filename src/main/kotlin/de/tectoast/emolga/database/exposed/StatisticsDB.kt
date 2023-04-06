package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.Command.Companion.byName
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.database.forAll
import de.tectoast.emolga.database.increment
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.atomic.AtomicReference

object StatisticsDB : Table("statistics") {
    val NAME = varchar("name", 30)
    val COUNT = integer("count")

    override val primaryKey = PrimaryKey(NAME)
    fun increment(name: String) = transaction {
        addLogger(Slf4jSqlDebugLogger)
        increment(name, mapOf(COUNT to 1))
    }

    fun buildDescription(e: GuildCommandEvent): String {
        val analysis = AtomicReference<String>()
        val otherCmds = mutableSetOf<String>()
        forAll {
            val name = it[NAME]
            val count = it[COUNT]
            if (name == "analysis") {
                analysis.set("Analysierte Replays: $count")
            } else {
                val c = byName(name.substring(4))
                if (c != null) {
                    if (c.checkBot(e.jda, e.guild.idLong)) otherCmds.add(c.prefix + c.name + ": " + count)
                }
            }
        }
        return analysis.get() + "\n" + otherCmds.joinToString("\n")
    }

    val analysisCount get() = transaction { select { NAME eq "analysis" }.first()[COUNT] }
}
