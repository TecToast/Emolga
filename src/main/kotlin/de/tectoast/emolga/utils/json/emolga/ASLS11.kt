package de.tectoast.emolga.utils.json.emolga

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.json.Emolga
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.Member

@Serializable
class ASLS11(
    val table: List<String> = emptyList(),
    val data: MutableMap<String, TeamData> = mutableMapOf(),
    val sid: String,
    val order: MutableList<Int> = mutableListOf(),
    private val originalorder: MutableList<Int> = mutableListOf(),
    val config: Config = Config(),
    var textChannel: Long = 820359155612254258,
    var currentCoach: Long = -1,
    var round: Int = 0
) {
    fun teamByCoach(mem: Long): TeamData? = data.values.firstOrNull { it.members[0]!! == mem }
    private fun teamnameByCoach(mem: Long): String = data.entries.first { it.value.members[0]!! == mem }.key
    fun addUserToTeam(user: Member, coach: Long, prize: Int) {
        val level = getLevelByMember(user)
        teamByCoach(coach)!!.apply {
            members[level] = user.idLong
            points -= prize
            //if(members.size == 5) order.values.forEach { l -> l.removeIf { it == table.indexOf(data.reverseGet(this))} }
            if (members.size == 5) originalorder.remove(table.indexOf(data.reverseGet(this)))
            prefix?.let { user.modifyNickname("[$prefix] ${user.effectiveName}").queue() }
            user.guild.addRoleToMember(user, user.jda.getRoleById(role)!!).queue()
        }
        val (x, y) = Emolga.get.asls11nametoid.indexOf(user.idLong).let { it.xdiv(24, 20) to it.ymod(24, 20) }
        table.indexOf(teamnameByCoach(coach)).let {
            RequestBuilder(sid).addRow(
                "Menschenhandel!${it.xmod(6, 2, 3)}${it.ydiv(6, 14 + level, 17)}",
                listOf("=$x$y", prize)
            ).addStrikethroughChange(0, "$x$y", true).execute()
        }
    }

    private fun teamByIndex(index: Int) = table[index].let { it to data[it]!! }

    fun isPlayer(mem: Member) = mem.roles.any { it.idLong in levelIds }
    fun isTaken(mem: Long) = data.values.any { datas -> datas.members.values.any { it == mem } }
    fun getLevelByMember(mem: Member): Int =
        levelIds.indexOfFirst { id -> mem.roles.any { it.idLong == id } } + 1

    fun nextCoach() {

        refillOrderIfEmpty()
        if (order.isEmpty()) {
            textChannel().sendMessage("Der Menschenhandel is vorbei :3").queue()
            return
        }
        val (teamname, teamData) = teamByIndex(order.removeFirst())
        val coach = teamData.members[0] ?: run {
            Command.sendToMe("lul guter Coach")
            return
        }
        textChannel().sendMessage("<@$coach> ($teamname) darf jemanden in den Ring werfen!").queue()
        currentCoach = coach
        saveEmolgaJSON()
    }

    private fun refillOrderIfEmpty() {
        if (order.isEmpty()) {
            order.addAll(originalorder.let {
                if (round++ % 2 != 0)
                    it.reversed()
                else it
            })
        }
    }

    private fun textChannel() = EmolgaMain.emolgajda.getTextChannelById(textChannel)!!

    companion object {
        val levelIds = listOf(
            1001222217595633757, 952663266344198174, 1001222414245568612, 1001222756429480069
        )
    }
}

@Suppress("unused")
@Serializable
class TeamData(
    val members: MutableMap<Int, Long> = mutableMapOf(),
    var points: Int = 6000,
    val role: Long,
    val prefix: String? = null
) {
    fun pointsToSpend(): Int = points - ((4 - members.size) * 100)

}

@Serializable
class Config(val waitFor: Long = 15000, val countdownSeconds: Int = 15)