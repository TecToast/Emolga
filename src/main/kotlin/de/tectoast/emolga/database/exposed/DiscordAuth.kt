package de.tectoast.emolga.database.exposed

import io.ktor.server.sessions.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DiscordAuth : Table("discordauth"), SessionStorage {
    val id = varchar("id", 32)
    val value = varchar("value", 256)
    override val primaryKey = PrimaryKey(id)

    override suspend fun read(id: String): String {
        return newSuspendedTransaction {
            select { DiscordAuth.id eq id }.firstOrNull()?.get(value) ?: throw NoSuchElementException()
        }
    }

    override suspend fun write(id: String, value: String) {
        newSuspendedTransaction {
            insertIgnore {
                it[this.id] = id
                it[this.value] = value
            }
        }
    }

    override suspend fun invalidate(id: String) {
        newSuspendedTransaction {
            deleteWhere { this.id eq id }
        }
    }
}
