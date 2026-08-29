package de.tectoast.emolga.domain.league.signup.repository

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

@Single
class LogoNameRepository(private val db: R2dbcDatabase) {
    suspend fun insertFileName(fileName: String, teamName: String? = null) {
        suspendTransaction(db) {
            LogoNameTable.upsert {
                it[LogoNameTable.filename] = fileName
                it[LogoNameTable.teamname] = teamName
            }
        }
    }

    suspend fun fileNameExists(checksum: String) = suspendTransaction(db) {
        LogoNameTable.selectAll().where { LogoNameTable.filename eq checksum }.count() > 0
    }
}

object LogoNameTable : Table("logo_name") {
    val filename = text("file_name")
    val teamname = text("team_name").nullable()

    override val primaryKey = PrimaryKey(filename)
}
