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
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.Consumer

object WarnsManager : DataManager("warns") {
    private val USERID = LongColumn("userid", this)
    private val MODID = LongColumn("modid", this)
    private val GUILDID = LongColumn("guildid", this)
    private val REASON = StringColumn("reason", this)
    private val TIMESTAMP = TimestampColumn("timestamp", this)

    init {
        setColumns(USERID, MODID, GUILDID, REASON, TIMESTAMP)
    }

    fun warn(userid: Long, modid: Long, guildid: Long, reason: String?) {
        insert(userid, modid, guildid, reason, null)
    }

    fun warnCount(userid: Long, guildid: Long): Int {
        return read(
            selectBuilder().count("warncount").where(and(USERID.check(userid), GUILDID.check(guildid))).build(this),
            ResultsFunction { s ->
                mapFirst(s) { set: ResultSet ->
                    unwrapCount(
                        set, "warncount"
                    )
                } ?: 0
            })
    }

    fun getWarnsFrom(userid: Long, guildid: Long): String {
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm")
        return read(selectAll(and(USERID.check(userid), GUILDID.check(guildid))), ResultsFunction { set ->
            map(set) { s: ResultSet ->
                "Von: <@${MODID.getValue(s)}>\nGrund: ${REASON.getValue(s)}\nZeitpunkt: ${
                    format.format(
                        Date(
                            TIMESTAMP.getValue(s).time
                        )
                    )
                } Uhr"
            }.joinToString(separator = "\n\n")
        })
    }

    fun getWarns(g: Guild): JSONArray {
        try {
            val set = GUILDID.getAll(g.idLong)
            val arr = JSONArray()
            val l: MutableList<JSONObject> = LinkedList()
            while (set!!.next()) {
                l.add(
                    JSONObject()
                        .put("userid", USERID.getValue(set))
                        .put("modid", MODID.getValue(set))
                        .put("reason", REASON.getValue(set))
                        .put("timestamp", TIMESTAMP.getValue(set).time)
                )
            }
            val idstocheck: MutableSet<Long> = HashSet()
            l.stream().map { j: JSONObject -> j.getLong("userid") }.forEach { e: Long -> idstocheck.add(e) }
            l.stream().map { j: JSONObject -> j.getLong("modid") }.forEach { e: Long -> idstocheck.add(e) }
            val names = HashMap<Long, String>()
            g.retrieveMembersByIds(idstocheck).get()
                .forEach(Consumer { mem: Member -> names[mem.idLong] = mem.effectiveName })
            for (j in l) {
                val uid = j.getLong("userid")
                val name = names[uid] ?: continue
                arr.put(
                    JSONObject().put("name", name).put("id", uid.toString()).put("reason", j.getString("reason"))
                        .put("mod", names[j.getLong("modid")]).put("timestamp", j.getLong("timestamp"))
                )
            }
            return arr
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return JSONArray().put(JSONObject().put("name", "ERROR"))
    }
}