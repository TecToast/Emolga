package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.koin.core.annotation.Single

@Single
class SixVsPokeworldAuthRepository(val db: R2dbcDatabase) {
    suspend fun isAuthorized(user: Long) = dbTransaction {
        SixVsPokeworldAuthTable.selectAll().where { SixVsPokeworldAuthTable.user eq user }.count() > 0
    }
}

object SixVsPokeworldAuthTable : Table("six_vs_pokeworld_auth") {
    val user = long("user")

    override val primaryKey = PrimaryKey(user)

}