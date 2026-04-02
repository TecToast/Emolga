package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

interface LogoNameRepository {
    suspend fun insertFileName(fileName: String, teamName: String? = null)
    suspend fun fileNameExists(checksum: String): Boolean
}

@Single(binds = [LogoNameRepository::class])
class PostgresLogoNameRepository(val db: R2dbcDatabase, val logoName: LogoNameDB) : LogoNameRepository {
    override suspend fun insertFileName(fileName: String, teamName: String?) {
        suspendTransaction(db) {
            logoName.upsert {
                it[logoName.FILENAME] = fileName
                it[logoName.TEAMNAME] = teamName
            }
        }
    }

    override suspend fun fileNameExists(checksum: String) = suspendTransaction(db) {
        logoName.selectAll().where { logoName.FILENAME eq checksum }.count() > 0
    }
}

@Single
class LogoNameDB : Table("logoname") {
    val FILENAME = varchar("filename", 32)
    val TEAMNAME = varchar("teamname", 64).nullable()

    override val primaryKey = PrimaryKey(FILENAME)
}
