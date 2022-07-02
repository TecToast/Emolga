package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.entities.Member

class TeamCommand : Command("team", "lol", CommandCategory.Various) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        setCustomPermissions(PermissionPreset.CULT)
    }

    override fun process(e: GuildCommandEvent) {
        e.guild.loadMembers().onSuccess { l: List<Member> ->
            val list = ArrayList<Member?>()
            for (mem in l) {
                if (mem.voiceState!!.inAudioChannel()) list.add(mem)
            }
            list.shuffle()
            e.reply(
                list[0]!!.effectiveName + " und " + list[1]!!.effectiveName + " **VS** " + list[2]!!
                    .effectiveName + " und " + list[3]!!.effectiveName
            )
        }
    }
}