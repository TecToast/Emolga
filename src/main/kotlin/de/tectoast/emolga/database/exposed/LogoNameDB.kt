package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert

object LogoNameDB : Table("logoname") {
    val FILENAME = varchar("filename", 32)
    val TEAMNAME = varchar("teamname", 64).nullable()

    override val primaryKey = PrimaryKey(FILENAME)

    suspend fun insertFileName(fileName: String, teamName: String? = null) = dbTransaction {
        upsert {
            it[FILENAME] = fileName
            it[TEAMNAME] = teamName
        }
    }

    suspend fun fileNameExists(checksum: String) = dbTransaction {
        selectAll().where { FILENAME eq checksum }.count() > 0
    }
}
