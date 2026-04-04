package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

@Single
class LogoNameRepository(val db: R2dbcDatabase) {
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

object LogoNameTable : Table("logoname") {
    val filename = varchar("filename", 32)
    val teamname = varchar("teamname", 64).nullable()

    override val primaryKey = PrimaryKey(filename)
}
