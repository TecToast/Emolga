package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.ktor.TeamgraphicsShape
import de.tectoast.emolga.ktor.TeamgraphicsSpriteStyle
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

interface TeamGraphicsMetaRepository {
    suspend fun getShape(guild: Long): TeamgraphicsShape?
    suspend fun getSpriteStyle(guild: Long): TeamgraphicsSpriteStyle?
}

@Single(binds = [TeamGraphicsMetaRepository::class])
class PostgresTeamGraphicsMetaRepository(
    private val db: R2dbcDatabase,
    private val teamGraphicsMeta: TeamGraphicsMetaDB
) : TeamGraphicsMetaRepository {
    override suspend fun getShape(guild: Long): TeamgraphicsShape? = suspendTransaction(db) {
        teamGraphicsMeta.select(teamGraphicsMeta.SHAPE).where { teamGraphicsMeta.GUILD eq guild }.firstOrNull()
            ?.get(teamGraphicsMeta.SHAPE)
    }

    override suspend fun getSpriteStyle(guild: Long): TeamgraphicsSpriteStyle? = suspendTransaction(db) {
        teamGraphicsMeta.select(teamGraphicsMeta.SPRITESTYLE).where { teamGraphicsMeta.GUILD eq guild }.firstOrNull()
            ?.get(teamGraphicsMeta.SPRITESTYLE)
    }
}

@Single
class TeamGraphicsMetaDB : Table("teamgraphicsmeta") {
    val GUILD = long("guild")
    val SHAPE = enumerationByName<TeamgraphicsShape>("shape", 16)
    val SPRITESTYLE = enumerationByName<TeamgraphicsSpriteStyle>("spritestyle", 16)

    override val primaryKey = PrimaryKey(GUILD)
}
