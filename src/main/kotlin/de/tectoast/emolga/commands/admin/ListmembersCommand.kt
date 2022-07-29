package de.tectoast.emolga.commands.admin

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class ListmembersCommand : Command("listmembers", "Zeigt alle User an, die diese Rolle haben", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "role",
                "Rolle",
                "Die ID der Rolle, die die User besitzen sollen",
                ArgumentManagerTemplate.DiscordType.ID
            )
            .setExample("!listmembers 760914483534889021")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val args = e.arguments
        val r = e.jda.getRoleById(args.getID("role")) ?: run {
            e.reply("Diese Rolle wurde nicht gefunden!")
            return
        }
        tco.guild.findMembers { it.roles.contains(r) }.onSuccess {
            e.reply("User mit der Rolle ${r.name}:\n${
                buildString {
                    append(it.joinToString("\n") { mem -> mem.effectiveName })
                    if (isEmpty()) {
                        tco.sendMessage("Kein Member hat die Rolle " + r.name + "!").queue()
                        return@onSuccess
                    }
                    append("\nInsgesamt: ").append(it.size)
                }
            }")
        }.onError {
            it.printStackTrace()
            e.reply("Es ist ein Fehler beim Laden der Member aufgetreten!")
        }
    }
}