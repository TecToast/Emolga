package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.select
import java.util.*

object LiveTeamDB : Table("liveteam") {
    val CODE = uuid("code")
    val LEAGUE = varchar("league", 100)

    suspend fun getByCode(code: UUID) = dbTransaction {
        select(LEAGUE).where { CODE eq code }.firstOrNull()?.get(LEAGUE)
    }

    override val primaryKey = PrimaryKey(CODE)
}