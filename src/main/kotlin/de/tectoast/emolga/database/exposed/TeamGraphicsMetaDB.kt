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

@Single
class TeamGraphicsMetaRepository(
    private val db: R2dbcDatabase,
) {
    suspend fun getShape(guild: Long): TeamgraphicsShape? = suspendTransaction(db) {
        TeamGraphicsMetaTable.select(TeamGraphicsMetaTable.SHAPE).where { TeamGraphicsMetaTable.GUILD eq guild }
            .firstOrNull()
            ?.get(TeamGraphicsMetaTable.SHAPE)
    }

    suspend fun getSpriteStyle(guild: Long): TeamgraphicsSpriteStyle? = suspendTransaction(db) {
        TeamGraphicsMetaTable.select(TeamGraphicsMetaTable.SPRITESTYLE).where { TeamGraphicsMetaTable.GUILD eq guild }
            .firstOrNull()
            ?.get(TeamGraphicsMetaTable.SPRITESTYLE)
    }
}

object TeamGraphicsMetaTable : Table("teamgraphicsmeta") {
    val GUILD = long("guild")
    val SHAPE = enumerationByName<TeamgraphicsShape>("shape", 16)
    val SPRITESTYLE = enumerationByName<TeamgraphicsSpriteStyle>("spritestyle", 16)

    override val primaryKey = PrimaryKey(GUILD)
}
