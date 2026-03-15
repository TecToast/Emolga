package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.ktor.TeamgraphicsShape
import de.tectoast.emolga.ktor.TeamgraphicsSpriteStyle
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.select

object TeamGraphicsMetaDB : Table("teamgraphicsmeta") {
    val GUILD = long("guild")
    val SHAPE = enumerationByName<TeamgraphicsShape>("shape", 16)
    val SPRITESTYLE = enumerationByName<TeamgraphicsSpriteStyle>("spritestyle", 16)

    override val primaryKey = PrimaryKey(GUILD)

    suspend fun getShape(guild: Long): TeamgraphicsShape? = dbTransaction {
        select(SHAPE).where { GUILD eq guild }.firstOrNull()?.get(SHAPE)
    }

    suspend fun getSpriteStyle(guild: Long): TeamgraphicsSpriteStyle? = dbTransaction {
        select(SPRITESTYLE).where { GUILD eq guild }.firstOrNull()?.get(SPRITESTYLE)
    }
}