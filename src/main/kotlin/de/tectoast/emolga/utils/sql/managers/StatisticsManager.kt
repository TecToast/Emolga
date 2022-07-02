package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.commands.Command.Companion.byName
import de.tectoast.emolga.commands.Command.Companion.updatePresence
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.columns.IntColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.util.concurrent.atomic.AtomicReference

object StatisticsManager : DataManager("statistics") {
    private val NAME = StringColumn("name", this)
    private val COUNT = IntColumn("count", this)

    init {
        setColumns(NAME, COUNT)
    }

    fun increment(name: String) {
        COUNT.increment(NAME, name)
        if (name == "analysis") updatePresence()
    }

    fun buildDescription(e: GuildCommandEvent): String {
        val analysis = AtomicReference<String>()
        val otherCmds: MutableList<String> = ArrayList()
        read(selectBuilder().orderBy(COUNT, "desc").build(this)) { s: ResultSet ->
            forEach(
                s
            ) { set: ResultSet ->
                val count = COUNT.getValue(set)
                val name = NAME.getValue(set)
                if (name == "analysis") analysis.set("Analysierte Replays: $count") else {
                    val c = byName(name.substring(4))
                    if (c != null) {
                        logger.info("name = $name")
                        if (c.checkBot(e.jda, e.guild.idLong)) otherCmds.add(c.prefix + c.name + ": " + count)
                    }
                }
            }
        }
        return """
               $analysis
               ${java.lang.String.join("\n", otherCmds)}
               """.trimIndent()
    }

    val analysisCount: Int
        get() = COUNT.retrieveValue(NAME, "analysis")!!

    private val logger = LoggerFactory.getLogger(StatisticsManager::class.java)

}