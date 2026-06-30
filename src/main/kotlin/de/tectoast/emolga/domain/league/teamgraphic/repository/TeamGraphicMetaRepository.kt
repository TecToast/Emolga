package de.tectoast.emolga.domain.league.teamgraphic.repository

import de.tectoast.emolga.domain.league.teamgraphic.model.TeamgraphicShape
import de.tectoast.emolga.domain.league.teamgraphic.model.TeamgraphicSpriteStyle
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class TeamGraphicMetaRepository(
    private val db: R2dbcDatabase,
) {
    suspend fun getShape(guild: Long): TeamgraphicShape? = suspendTransaction(db) {
        TeamGraphicMetaTable.select(TeamGraphicMetaTable.shape).where { TeamGraphicMetaTable.guild eq guild }
            .firstOrNull()
            ?.get(TeamGraphicMetaTable.shape)
    }

    suspend fun getSpriteStyle(guild: Long): TeamgraphicSpriteStyle? = suspendTransaction(db) {
        TeamGraphicMetaTable.select(TeamGraphicMetaTable.spriteStyle).where { TeamGraphicMetaTable.guild eq guild }
            .firstOrNull()
            ?.get(TeamGraphicMetaTable.spriteStyle)
    }
}

object TeamGraphicMetaTable : Table("teamgraphicsmeta") {
    val guild = long("guild")
    val shape = enumerationByName<TeamgraphicShape>("shape", 16)
    val spriteStyle = enumerationByName<TeamgraphicSpriteStyle>("spritestyle", 16)

    override val primaryKey = PrimaryKey(guild)
}



