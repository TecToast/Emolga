package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.json.LogoChecksum
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

object LogoChecksumDB : Table("logo_checksum") {
    val CHECKSUM = varchar("checksum", 32)
    val FILEID = varchar("fileid", 64)

    override val primaryKey = PrimaryKey(CHECKSUM)

    suspend fun insertData(data: LogoChecksum) = dbTransaction {
        insert {
            it[CHECKSUM] = data.checksum
            it[FILEID] = data.fileId
        }
    }

    suspend fun findByChecksum(checksum: String) = dbTransaction {
        selectAll().where { CHECKSUM eq checksum }.firstOrNull()?.let { LogoChecksum(it[CHECKSUM], it[FILEID]) }
    }
}
