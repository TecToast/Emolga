package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.defaultTimeFormat
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object WarnsDB : Table("warns") {
    val USERID = long("userid")
    val MODID = long("modid")
    val GUILDID = long("guildid")
    val REASON = varchar("reason", 1000)
    val TIMESTAMP = timestamp("timestamp")

    fun warn(userid: Long, modid: Long, guildid: Long, reason: String?) = transaction {
        insert {
            it[USERID] = userid
            it[MODID] = modid
            it[GUILDID] = guildid
            it[REASON] = reason ?: "Kein Grund angegeben"
            it[TIMESTAMP] = Instant.now()
        }
    }

    fun warnCount(userid: Long, guildid: Long) = transaction {
        select { (USERID eq userid) and (GUILDID eq guildid) }.count()
    }

    fun getWarnsFrom(userid: Long, guildid: Long) = transaction {
        select { (USERID eq userid) and (GUILDID eq guildid) }.joinToString(separator = "\n\n") {
            "Von: <@${it[MODID]}>\nGrund: ${it[REASON]}\nZeitpunkt: ${defaultTimeFormat.format(it[TIMESTAMP].toEpochMilli())}"
        }
    }
}
