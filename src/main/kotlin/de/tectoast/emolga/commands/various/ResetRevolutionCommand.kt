package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.entities.Member
import java.util.regex.Pattern

class ResetRevolutionCommand : Command("resetrevolution", "Setzt die Diktatur zurück", CommandCategory.Various) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        setCustomPermissions(PermissionPreset.CULT)
    }

    override fun process(e: GuildCommandEvent) {
        val g = e.guild
        e.textChannel.sendMessage("Die **Diktatur** ist zu Ende D:").queue()
        val o = emolgaJSON.getJSONObject("revolutionreset").getJSONObject(g.id)
        for (s in o.keySet()) {
            g.retrieveMemberById(s!!).submit()
                .thenCompose { mem: Member -> mem.modifyNickname(o.getString(s)).submit() }
        }
        for (gc in g.channels) {
            gc.manager.setName(REVOLUTION_PATTERN.matcher(gc.name).replaceFirst("")).queue()
        }
        g.selfMember.modifyNickname("Emolga").queue()
        val arr = emolgaJSON.getJSONArray("activerevolutions")
        arr.remove(arr.toList().indexOf(g.idLong))
        saveEmolgaJSON()
    }

    companion object {
        private val REVOLUTION_PATTERN = Pattern.compile("(.*)-")
    }
}