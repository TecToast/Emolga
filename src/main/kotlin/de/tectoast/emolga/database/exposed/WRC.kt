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

    suspend fun getAllSignupsForGameday(wrcname: String, gameday: Int) = dbTransaction {
        selectAll().where { (WRCNAME eq wrcname) and (GAMEDAY eq gameday) }.map { it[USERID] to it[WARRIOR] }
    }

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
        val (warriors, challengers) = selectAll().where { (WRCNAME eq wrcname) and (GAMEDAY eq gameday) }
            .map { it[USERID] to it[WARRIOR] }.partition { it.second }
        val allRegisteredUsers =
            select(USERID).withDistinct().where { (WRCNAME eq wrcname) and (REGISTERED eq true) }.map { it[USERID] }
                .toSet()
        Embed {
            color = embedColor
            title = "$wrcname Anmeldung Spieltag $gameday"
            field("Warriors", warriors.toEmbedFieldValue(allRegisteredUsers))
            field("Challengers", challengers.toEmbedFieldValue(allRegisteredUsers))
        }
    }

    fun buildSignupButton(wrcName: String, gameday: Int, disabled: Boolean) =
        WRCSignupButton(
            label = if (disabled) "Anmeldung geschlossen" else "An/abmelden",
            emoji = Emoji.fromUnicode("✅").takeUnless { disabled },
            disabled = disabled
        ) {
            this.wrcname = wrcName
            this.gameday = gameday
        }

    private fun List<Pair<Long, Boolean>>.toEmbedFieldValue(allRegisterdUsers: Set<Long>): String {
        val (new, alreadyRegistered) = partition { it.first !in allRegisterdUsers }
        return buildString {
            append(new.joinToString("\n") { "<@${it.first}>" })
            if (alreadyRegistered.isNotEmpty()) {
                appendLine()
                append("**Bereits teilgenommen:**")
                appendLine()
                append(alreadyRegistered.joinToString("\n") { "<@${it.first}>" })
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
