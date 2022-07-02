package de.tectoast.emolga.commands.admin

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.entities.Member

class ListmembersCommand : Command("listmembers", "Zeigt alle User an, die diese Rolle haben", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("role", "Rolle", "Die Rolle, die die User besitzen sollen", ArgumentManagerTemplate.DiscordType.ID)
            .setExample("!listmembers 760914483534889021")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val args = e.arguments!!
        val r = e.jda.getRoleById(args.getID("role"))
        tco.guild.findMembers { mem: Member -> mem.roles.contains(r) }.onSuccess { members: List<Member> ->
            val s = StringBuilder()
            for (mem in members) {
                s.append(mem.effectiveName).append("\n")
            }
            if (s.toString().isEmpty()) {
                tco.sendMessage("Kein Member hat die Rolle " + r!!.name + "!").queue()
                return@onSuccess
            }
            s.append("Insgesamt: ").append(members.size)
            tco.sendMessage(
                """
    User mit der Rolle ${r!!.name}:
    $s
    """.trimIndent()
            ).queue()
        }.onError { t: Throwable ->
            t.printStackTrace()
            tco.sendMessage("Es ist ein Fehler beim Laden der Member aufgetreten!").queue()
        }
    }
}