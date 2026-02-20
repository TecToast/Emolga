package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll

object SixVsPokeworldAuthDB : Table("six_vs_pokeworld_auth") {
    val USER = long("user")

    override val primaryKey = PrimaryKey(USER)

    suspend fun isAuthorized(user: Long) = dbTransaction {
        selectAll().where { USER eq user }.count() > 0
    }
}