package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.JDA
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object MuteDB : Table("mutes") {
    val USERID = long("userid")
    val MODID = long("modid")
    val GUILDID = long("guildid")
    val REASON = text("reason")
    val TIMESTAMP = timestamp("timestamp")
    val EXPIRES = timestamp("expires").nullable()

    fun mute(userid: Long, modid: Long, guildid: Long, reason: String, expires: Long?) = transaction {
        insert {
            it[this.USERID] = userid
            it[this.MODID] = modid
            it[this.GUILDID] = guildid
            it[this.REASON] = reason
            it[this.TIMESTAMP] = Instant.now()
            it[this.EXPIRES] = expires?.let { ex -> Instant.ofEpochMilli(ex) }
        }
    }

    fun unmute(userid: Long, guildid: Long) = transaction {
        deleteWhere { (this.USERID eq userid) and (this.GUILDID eq guildid) }
    }

    fun schedule(jda: JDA) = transaction {
        selectAll().forEach {
            jda.getGuildById(it[GUILDID])?.run {
                Command.muteTimer(
                    this, it[EXPIRES]?.toEpochMilli() ?: -1, it[USERID]
                )
            }
        }
    }
}
