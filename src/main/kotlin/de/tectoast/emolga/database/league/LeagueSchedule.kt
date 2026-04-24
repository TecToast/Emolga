package de.tectoast.emolga.database.league

import de.tectoast.emolga.utils.referencesCascade
import org.jetbrains.exposed.v1.core.Table

object LeagueScheduleTable : Table("league_schedule") {
    val id = integer("id").autoIncrement()
    val leagueName = varchar("league_name", 255).referencesCascade(LeagueCoreTable.leagueName)
    val week = integer("week")
    val battleIndex = integer("battle_index")

    val p1 = integer("p1")
    val p2 = integer("p2")

    override val primaryKey = PrimaryKey(id)

    init {
        index(true, leagueName, week, battleIndex)
        index(false, leagueName, p1, p2)
    }
}
