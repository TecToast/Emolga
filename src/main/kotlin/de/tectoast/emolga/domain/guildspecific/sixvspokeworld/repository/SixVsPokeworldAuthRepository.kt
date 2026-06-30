package de.tectoast.emolga.domain.guildspecific.sixvspokeworld.repository

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class SixVsPokeworldAuthRepository(private val db: R2dbcDatabase) {
    suspend fun isAuthorized(user: Long) = suspendTransaction(db) {
        SixVsPokeworldAuthTable.selectAll().where { SixVsPokeworldAuthTable.user eq user }.count() > 0
    }
}

object SixVsPokeworldAuthTable : Table("six_vs_pokeworld_auth") {
    val user = long("user")

    override val primaryKey = PrimaryKey(user)
}

