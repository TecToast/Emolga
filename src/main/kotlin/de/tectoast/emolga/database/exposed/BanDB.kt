package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.JDA
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object BanDB : Table("bans") {
    // create the columns based on BanManager.kt
    val USERID = long("userid")
    val USERNAME = varchar("username", 32)
    val MODID = long("modid")
    val GUILDID = long("guildid")
    val REASON = varchar("reason", 256)
    val TIMESTAMP = timestamp("timestamp")
    val EXPIRES = timestamp("expires").nullable()

    // create a function to insert a ban
    fun ban(userid: Long, username: String, modid: Long, guildid: Long, reason: String, expires: Instant?) {
        insert {
            it[this.USERID] = userid
            it[this.USERNAME] = username
            it[this.MODID] = modid
            it[this.GUILDID] = guildid
            it[this.REASON] = reason
            it[this.TIMESTAMP] = Instant.now()
            it[this.EXPIRES] = expires
        }
    }


    fun unban(userid: Long, guildid: Long) = transaction {
        deleteWhere { (this.USERID eq userid) and (this.GUILDID eq guildid) }
    }

    fun schedule(jda: JDA) = transaction {
        selectAll().forEach {
            jda.getGuildById(it[GUILDID])?.run {
                Command.banTimer(
                    this, it[EXPIRES]?.toEpochMilli() ?: -1, it[USERID]
                )
            }
        }
    }
}
