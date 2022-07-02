package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.entities.Member
import java.util.stream.Collectors

class GetIdsCommand : Command("getids", "Holt die Namen und die IDs der Leute mit der Rolle", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "role",
                "Rolle",
                "Die Rolle, von der die IDs geholt werden sollen",
                ArgumentManagerTemplate.DiscordType.ID
            )
            .setExample("!getids 1234567889990076868")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val r = e.jda.getRoleById(e.arguments!!.getID("role"))
        tco.guild.findMembers { mem: Member -> mem.roles.contains(r) }.onSuccess { members: List<Member> ->
            tco.sendMessage(members.stream().map { mem: Member -> mem.effectiveName + ": " + mem.id }
                .collect(Collectors.joining("\n"))).queue()
        }.onError { t: Throwable ->
            t.printStackTrace()
            tco.sendMessage("Es ist ein Fehler beim Laden der Member aufgetreten!").queue()
        }
    }
}