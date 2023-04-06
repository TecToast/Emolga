package de.tectoast.emolga.database.exposed

import io.ktor.server.sessions.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DiscordAuthDB : Table("discordauth"), SessionStorage {
    val ID = varchar("id", 32)
    val VALUE = varchar("value", 256)
    override val primaryKey = PrimaryKey(ID)

    override suspend fun read(id: String): String {
        return newSuspendedTransaction {
            select { ID eq id }.firstOrNull()?.get(VALUE) ?: throw NoSuchElementException()
        }
    }

    override suspend fun write(id: String, value: String) {
        newSuspendedTransaction {
            insertIgnore {
                it[this.ID] = id
                it[this.VALUE] = value
            }
        }
    }

    override suspend fun invalidate(id: String) {
        newSuspendedTransaction {
            deleteWhere { this.ID eq id }
        }
    }
}
