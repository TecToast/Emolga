package de.tectoast.emolga.utils.json.emolga

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.indexedBy
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.xmod
import de.tectoast.emolga.utils.ydiv
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.Member
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.updateOne

@Serializable
class ASLCoachData(
    @SerialName("_id") @Contextual val id: Id<ASLCoachData>,
    val table: List<String> = emptyList(),
    val data: Map<String, TeamData> = mutableMapOf(),
    private val sid: String,
    val order: MutableList<Int> = mutableListOf(),
    val originalorder: MutableList<Int> = mutableListOf(),
    val config: Config = Config(),
    var textChannel: Long = 820359155612254258,
    var currentCoach: Long = -1,
    var round: Int = 0
) {
    fun teamByCoach(mem: Long): TeamData? = data.values.firstOrNull { it.members[0]!! == mem }
    fun indexOfMember(mem: Long): Triple<Int, Int, String> = data.entries.first { mem in it.value.members.values }
        .let { en -> Triple(en.value.members.entries.first { it.value == mem }.key, en.key.indexedBy(table), en.key) }

    fun roleIdByMember(mem: Long) = data.values.first { mem in it.members.values }.role

    fun teammembersByMember(mem: Long) = data.values.first { mem in it.members.values }.members.values
    private fun teamnameByCoach(mem: Long): String = data.entries.first { it.value.members[0]!! == mem }.key
    suspend fun addUserToTeam(user: Member, coach: Long, prize: Int) {
        val level = getLevelByMember(user)
        teamByCoach(coach)!!.apply {
            members[level] = user.idLong
            points -= prize
            //if(members.size == 5) order.values.forEach { l -> l.removeIf { it == table.indexOf(data.reverseGet(this))} }
            if (members.size == TEAMSIZE) {
                val toremove = table.indexOf(data.reverseGet(this))
                originalorder.remove(toremove)
                order.remove(toremove)
            }
            try {
                prefix?.let {
                    user.modifyNickname("[$it] ${user.effectiveName.substringAfterLast("]").trim()}").queue(
                        {},
                    ) { ex ->
                        ex.printStackTrace()
                    }
                }
                user.guild.addRoleToMember(user, user.jda.getRoleById(role)!!).queue()
            } catch (ex: Exception) {
                ex.printStackTrace()
                if (ex is CancellationException) throw ex
            }
        }

        insertIntoDoc(user, coach, level, prize)
    }


    private fun teamByIndex(index: Int) = table[index].let { it to data[it]!! }

    suspend fun isPlayer(mem: Member) = mem.idLong in participants()
    fun isTaken(mem: Long) = data.values.any { datas -> (1..3).any { datas.members[it] == mem } }

    suspend fun getLevelByMember(mem: Member): Int = participants()[mem.idLong]!!

    fun nextCoach() {

        refillOrderIfEmpty()
        if (order.isEmpty()) {
            textChannel().sendMessage("Der Menschenhandel is vorbei :3").queue()
            return
        }
        val (teamname, teamData) = teamByIndex(order.removeFirst())
        val coach = teamData.members[0] ?: run {
            SendFeatures.sendToMe("lul guter Coach")
            return
        }
        textChannel().sendMessage("<@$coach> ($teamname) darf jemanden in den Ring werfen!").queue()
        currentCoach = coach
    }

    private fun refillOrderIfEmpty() {
        if (order.isEmpty()) {
            order.addAll(originalorder.let {
                if (round++ % 2 != 0) it.reversed()
                else it
            })
        }
    }

    private fun textChannel() = jda.getTextChannelById(textChannel)!!

    suspend fun save() = db.aslcoach.updateOne(this)
    private suspend fun insertIntoDoc(
        user: Member, coach: Long, level: Int, prize: Int
    ) {
        val row = participants().keys.toList().indexOf(user.idLong) + 4
        table.indexOf(teamnameByCoach(coach)).let {
            RequestBuilder(sid).addRow(
                "Menschenhandel!${it.xmod(6, 3, 2)}${it.ydiv(6, 16, 14 + level)}", listOf("=T$row", prize)
            ).addStrikethroughChange(959039009, "T$row", true).execute()
        }
    }

    companion object {
        const val TEAMSIZE = 4

        private var _participants: Map<Long, Int>? = null

        private val mutex = Mutex()
        suspend fun participants(): Map<Long, Int> {
            init()
            return _participants!!
        }

        private suspend fun init() {
            mutex.withLock {
                if (_participants == null) {
                    val data = db.signups.get(518008523653775366)!!
                    val groupBy = data.users.entries.groupBy { it.value.conference }
                    _participants = buildMap {
                        for ((index, conference) in data.conferences.withIndex()) {
                            val confnum = conference.toIntOrNull() ?: continue
                            putAll(groupBy[conference]!!.map { it.key }.associateWith { confnum })
                            put(index.toLong(), -1)
                        }
                    }
                }
            }
        }
    }
}


@Serializable
class TeamData(
    val members: MutableMap<Int, Long> = mutableMapOf(),
    var points: Int = 6000,
    val role: Long,
    val prefix: String? = null
) {
    fun pointsToSpend(): Int = points - ((ASLCoachData.TEAMSIZE - 1 - members.size) * 100)

}

@Serializable
class Config(val waitFor: Long = 15000, val countdownSeconds: Int = 15, val sendOn: List<Int> = listOf(5, 15))

fun <T, R> Map<T, R>.reverseGet(value: R): T? = this.entries.firstOrNull { it.value == value }?.key
