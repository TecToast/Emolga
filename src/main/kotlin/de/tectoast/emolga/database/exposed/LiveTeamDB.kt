package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object LiveTeamDB : Table("liveteam") {
    val CODE = uuid("code")
    val LEAGUE = varchar("league", 100)

    suspend fun getByCode(code: Uuid) = dbTransaction {
        select(LEAGUE).where { CODE eq code }.firstOrNull()?.get(LEAGUE)
    }

    suspend fun generateForLeague(league: String): Uuid {
        val code = Uuid.random()
        dbTransaction {
            insert {
                it[CODE] = code
                it[LEAGUE] = league
            }
        }
        return code
    }

    override val primaryKey = PrimaryKey(CODE)
}
