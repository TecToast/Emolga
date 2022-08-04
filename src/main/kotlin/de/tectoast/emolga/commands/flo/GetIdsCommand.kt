package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

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

    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val r = e.jda.getRoleById(e.arguments.getID("role"))
        tco.guild.findMembers { it.roles.contains(r) }.onSuccess { members ->
            tco.sendMessage(members.joinToString("\n") { it.effectiveName + ": " + it.id }).queue()
        }.onError {
            it.printStackTrace()
            tco.sendMessage("Es ist ein Fehler beim Laden der Member aufgetreten!").queue()
        }
    }
}