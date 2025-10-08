package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.features.wrc.WRCSignupButton
import de.tectoast.emolga.utils.embedColor
import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.jetbrains.exposed.v1.core.ReferenceOption.CASCADE
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object WRCDataDB : Table("wrc_running") {
    val WRCNAME = varchar("wrcname", 100)
    val WARRIORROLE = long("warriorrole")
    val GUILD = long("guild")
    val TLIDENTIFIER = varchar("tlidentifier", 100).nullable()
    val SID = varchar("sid", 100)
    val SIGNUPCHANNEL = long("signupchannel")
    val LASTSIGNUP = timestamp("lastsignup")
    val SIGNUPDURATIONMINS = integer("signupdurationmins")
    val INTERVALMINS = integer("intervalmins")
    val GAMEDAYS = integer("gamedays")

    suspend fun getByName(name: String) = dbTransaction {
        selectAll().where { WRCNAME eq name }.firstOrNull()
    }

    override val primaryKey = PrimaryKey(WRCNAME)
}

object WRCUserSignupDB : Table("wrc_usersignup") {
    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val USERID = long("userid")
    val WARRIOR = bool("warrior")
    val REGISTERED = bool("registered").default(false)

    init {
        foreignKey(WRCNAME to WRCDataDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }

    override val primaryKey = PrimaryKey(WRCNAME, GAMEDAY, USERID)

    suspend fun unsignupUser(wrcname: String, gameday: Int, userid: Long) = dbTransaction {
        deleteWhere { (WRCNAME eq wrcname) and (GAMEDAY eq gameday) and (USERID eq userid) } > 0
    }

    suspend fun signupUser(name: String, gameday: Int, uid: Long, warrior: Boolean) = dbTransaction {
        insert {
            it[WRCNAME] = name
            it[GAMEDAY] = gameday
            it[USERID] = uid
            it[WARRIOR] = warrior
        }
    }

    suspend fun buildSignupEmbed(wrcname: String, gameday: Int) = dbTransaction {
        val (warriors, challengers) = getAllSignupsForGameday(wrcname, gameday)
        val allRegisteredUsers = getAllRegisteredUsers(wrcname)
        Embed {
            color = embedColor
            title = "$wrcname Anmeldung Spieltag $gameday"
            field("Warriors", warriors.toEmbedFieldValue(allRegisteredUsers))
            field("Challengers", challengers.toEmbedFieldValue(allRegisteredUsers))
        }
    }

    suspend fun getAllSignupsForGameday(
        wrcname: String, gameday: Int
    ): Pair<List<Long>, List<Long>> = dbTransaction {
        selectAll().where { (WRCNAME eq wrcname) and (GAMEDAY eq gameday) }.map { it[USERID] to it[WARRIOR] }
            .partition { it.second }.let { it.first.map { i -> i.first } to it.second.map { i -> i.first } }
    }

    suspend fun getAllRegisteredUsers(wrcname: String): Set<Long> = dbTransaction {
        select(USERID).withDistinct().where { (WRCNAME eq wrcname) and (REGISTERED eq true) }.map { it[USERID] }.toSet()
    }

    fun buildSignupButton(wrcName: String, gameday: Int, disabled: Boolean) = WRCSignupButton(
        label = if (disabled) "Anmeldung geschlossen" else "An/abmelden",
        emoji = Emoji.fromUnicode("✅").takeUnless { disabled },
        disabled = disabled
    ) {
        this.wrcname = wrcName
        this.gameday = gameday
    }

    private fun List<Long>.toEmbedFieldValue(allRegisterdUsers: Set<Long>): String {
        val (new, alreadyRegistered) = partition { it !in allRegisterdUsers }
        return buildString {
            append(new.joinToString("\n") { "<@${it}>" })
            if (alreadyRegistered.isNotEmpty()) {
                appendLine()
                append("**Bereits teilgenommen:**")
                appendLine()
                append(alreadyRegistered.joinToString("\n") { "<@${it}>" })
            }
        }.ifEmpty { "_keine_" }
    }

}

object WRCSignupMessageDB : Table("wrc_signupmessage") {

    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val MESSAGEID = long("messageid")

    override val primaryKey = PrimaryKey(WRCNAME, GAMEDAY)

    suspend fun setMessageIdForGameday(wrcName: String, gameday: Int, mid: Long) = dbTransaction {
        upsert {
            it[WRCNAME] = wrcName
            it[GAMEDAY] = gameday
            it[MESSAGEID] = mid
        }
    }

    suspend fun getMessageIdForGameday(wrcName: String, gameday: Int) = dbTransaction {
        select(MESSAGEID).where { (WRCNAME eq wrcName) and (GAMEDAY eq gameday) }.firstOrNull()?.get(MESSAGEID)
    }

    init {
        foreignKey(WRCNAME to WRCDataDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }
}

object WRCMatchupsDB : Table("wrc_matchups") {

    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val INDEX = integer("index")
    val U1 = long("u1")
    val U2 = long("u2")

    override val primaryKey = PrimaryKey(WRCNAME, GAMEDAY, INDEX)

    suspend fun insertMatchup(wrcName: String, gameday: Int, index: Int, u1: Long, u2: Long) = dbTransaction {
        insert {
            it[WRCNAME] = wrcName
            it[GAMEDAY] = gameday
            it[INDEX] = index
            it[U1] = u1
            it[U2] = u2
        }
    }

    init {
        foreignKey(WRCNAME to WRCDataDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }
}

object WRCMonsOptionsDB : Table("wrc_monsoptions") {
    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val MON = varchar("mon", 100)

    override val primaryKey = PrimaryKey(WRCNAME, GAMEDAY, MON)

    init {
        foreignKey(WRCNAME to WRCDataDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }
}

object WRCMonsPickedDB : Table("wrc_monspicked") {
    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val USERID = long("userid")
    val MON = varchar("mon", 100)

    override val primaryKey = PrimaryKey(WRCNAME, GAMEDAY, USERID, MON)

    init {
        foreignKey(WRCNAME to WRCDataDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }
}
