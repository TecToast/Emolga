@file:OptIn(ExperimentalSerializationApi::class)

package de.tectoast.emolga.utils.json.emolga

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.*
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Member
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.updateOne
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

@Serializable
class ASLCoachData(
    @SerialName("_id") @Contextual val id: Id<ASLCoachData>,
    val table: List<String> = emptyList(),
    val data: Map<String, TeamData> = mutableMapOf(),
    private val sid: String,
    @EncodeDefault
    val order: MutableList<Int> = mutableListOf(),
    val originalorder: MutableList<Int> = mutableListOf(),
    val config: Config = Config(),
    var textChannel: Long = 820359155612254258,
    var currentCoach: Long = -1,
    @EncodeDefault
    var round: Int = 1
) {
    fun teamByCoach(mem: Long): TeamData? = data.values.firstOrNull { it.members[0]!! == mem }

    fun teamnameByCoach(mem: Long): String = data.entries.first { it.value.members[0]!! == mem }.key
    suspend fun addUserToTeam(member: Member, coach: Long, prize: Int) {
        val level = getLevelByMember(member)
        teamByCoach(coach)!!.apply {
            members[level] = member.idLong
            points -= prize
            if (members.size >= config.teamSize) {
                val toremove = table.indexOf(data.reverseGet(this))
                originalorder.remove(toremove)
                order.remove(toremove)
            }
            try {
                prefix?.let {
                    val newName = "[$it] ${member.effectiveName}"
                    if (newName.length <= 32 && member.guild.selfMember.canInteract(member)) {
                        member.modifyNickname("[$it] ${member.effectiveName.substringAfterLast("]").trim()}").queue(
                            {},
                        ) { ex ->
                            logger.error("Couldnt modify nickname", ex)
                        }
                    }
                }
                member.guild.addRoleToMember(member, member.jda.getRoleById(role)!!)
                    .queueAfter(3000, TimeUnit.MILLISECONDS)
            } catch (ex: Exception) {
                logger.error("unknown", ex)
                if (ex is CancellationException) throw ex
            }
        }
        insertIntoDoc(member, coach, level, prize)
    }


    private fun teamByIndex(index: Int) = table[index].let { it to data[it]!! }

    suspend fun isPlayer(mem: Member) = mem.idLong in participants()
    fun isTaken(mem: Long) = data.values.any { datas -> (1..<config.teamSize).any { datas.members[it] == mem } }

    suspend fun getLevelByMember(mem: Member): Int = participants()[mem.idLong]!!

    fun nextCoach() {
        refillOrderIfEmpty()
        if (order.isEmpty()) return textChannel().sendMessage("Der Menschenhandel ist vorbei :3").queue()
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
        val groupBy = groupedParticipants()
        val entries = groupBy[level.toString()]!!
        val siteCoord =
            level.minus(1)
                .coordXMod("Menschenhandel", 2, 2, 20, 14, 7 + entries.indexOfFirst { user.idLong in it.users })
                .substringAfter("!")
        table.indexOf(teamnameByCoach(coach)).let {
            RequestBuilder(sid).addRow(
                "Menschenhandel!${it.xmod(6, 3, 2)}${it.ydiv(6, 17, 14 + level)}", listOf("=$siteCoord", prize)
            ).addStrikethroughChange(85312398, siteCoord, true)
                .addFGColorChange(85312398, siteCoord, 0xFF0000.convertColor()).execute()
        }
    }

    companion object {

        val groupedParticipants = OneTimeCache {
            val data = db.signups.get(Constants.G.ASL)!!
            data.users.groupBy { it.conference!! }
        }

        val participants = OneTimeCache {
            val groupBy = groupedParticipants()
            val data = db.signups.get(Constants.G.ASL)!!
            buildMap {
                for ((index, conference) in data.conferences.withIndex()) {
                    val confnum = conference.toIntOrNull() ?: continue
                    if (confnum == 0) continue
                    putAll(groupBy[conference]!!.map { it.users.first() }.associateWith { confnum })
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
    fun pointsToSpend(data: ASLCoachData): Int = points - ((data.config.teamSize - 1 - members.size) * 100)

}

@Serializable
class Config(
    val waitFor: Long = 15000,
    val countdownSeconds: Int = 15,
    val sendOn: List<Int> = listOf(5, 15),
    val teamSize: Int = 5
)

fun <T, R> Map<T, R>.reverseGet(value: R): T? = this.entries.firstOrNull { it.value == value }?.key
