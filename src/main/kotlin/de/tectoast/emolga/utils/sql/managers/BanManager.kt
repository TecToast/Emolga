package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.sql.base.Condition.and
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.DataManager.ResultsFunction
import de.tectoast.emolga.utils.sql.base.columns.LongColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn
import de.tectoast.jsolf.JSONArray
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp

object BanManager : DataManager("bans") {
    private val USERID = LongColumn("userid", this)
    private val USERNAME = StringColumn("username", this)
    private val MODID = LongColumn("modid", this)
    private val GUILDID = LongColumn("guildid", this)
    private val REASON = StringColumn("reason", this)
    private val TIMESTAMP = TimestampColumn("timestamp", this)
    private val EXPIRES = TimestampColumn("expires", this)

    init {
        setColumns(USERID, USERNAME, MODID, GUILDID, REASON, TIMESTAMP, EXPIRES)
    }

    fun ban(userid: Long, username: String, modid: Long, guildid: Long, reason: String, expires: Timestamp?) {
        insert(userid, username, modid, guildid, reason, null, expires)
    }

    fun getBans(g: Guild): JSONArray {
        try {
            val l: List<JSONObject> = read(selectAll(GUILDID.check(g.idLong)), ResultsFunction { s ->
                map(s) { set: ResultSet ->
                    JSONObject()
                        .put("userid", USERID.getValue(set))
                        .put("username", USERNAME.getValue(set))
                        .put("modid", MODID.getValue(set))
                        .put("reason", REASON.getValue(set))
                        .put("timestamp", TIMESTAMP.getValue(set).time)
                }
            })
            val arr = JSONArray()
            val names = HashMap<Long, String>()
            g.retrieveMembersByIds(l.map { it.getLong("modid") }).get()
                .forEach { mem: Member -> names[mem.idLong] = mem.effectiveName }
            for (j in l) {
                val uid = j.getLong("userid")
                arr.put(
                    JSONObject().put("name", j.getString("username")).put("id", uid.toString())
                        .put("reason", j.getString("reason")).put("mod", names[j.getLong("modid")])
                        .put("timestamp", j.getLong("timestamp"))
                )
            }
            return arr
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return JSONArray().put(JSONObject().put("name", "ERROR"))
    }

    fun unbanWebsite(g: Guild, userid: Long): JSONObject {
        val o = JSONObject()
        logger.info("userid = $userid")
        logger.info("g.getIdLong() = " + g.idLong)
        g.unban(UserSnowflake.fromId(userid)).queue()
        return if (unban(userid, g.idLong) > 0) o.put("success", "Entbannung erfolgreich!") else o.put(
            "error",
            "Die Person war gar nicht gebannt!"
        )
    }

    fun unban(userid: Long, guildid: Long): Int {
        return delete(and(USERID.check(userid), GUILDID.check(guildid)))
    }

    private val logger = LoggerFactory.getLogger(BanManager::class.java)
}