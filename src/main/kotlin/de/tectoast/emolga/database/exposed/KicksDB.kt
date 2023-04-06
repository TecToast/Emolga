package de.tectoast.emolga.database.exposed


import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object KicksDB : Table("kicks") {
    val USERID = long("userid")
    val MODID = long("modid")
    val GUILDID = long("guildid")
    val REASON = text("reason")
    val TIMESTAMP = timestamp("timestamp")

    fun kick(userid: Long, modid: Long, guildid: Long, reason: String) {
        insert {
            it[this.USERID] = userid
            it[this.MODID] = modid
            it[this.GUILDID] = guildid
            it[this.REASON] = reason
            it[this.TIMESTAMP] = Instant.now()
        }
    }
}
