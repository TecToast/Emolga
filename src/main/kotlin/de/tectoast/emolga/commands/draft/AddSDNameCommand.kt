package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.SDNamesManager
import net.dv8tion.jda.api.entities.User

class AddSDNameCommand : Command("addsdname", "Registriert deinen Showdown-Namen bei Emolga", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "SD-Name", "Der SD-Name", ArgumentManagerTemplate.Text.any())
            .add("id", "Die ID (nur Flo)", "Nur für Flo", ArgumentManagerTemplate.DiscordType.ID, true)
            .setExample("!addsdname TecToast")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val args = e.arguments!!
        if (args.has("id") && e.isNotFlo) {
            e.reply("Nur Flo darf den Command mit einer ID verwenden!")
            return
        }
        val name = args.getText("name")
        val b = SDNamesManager.addIfAbsent(name, args.getOrDefault("id", e.author.idLong))
        if (b) {
            if (args.has("id")) {
                e.jda.retrieveUserById(args.getID("id"))
                    .queue { mem: User -> e.reply("Der Name `%s` wurde für %s registriert!".formatted(name, mem.name)) }
            } else {
                e.reply("Der Name `%s` wurde für dich registriert!".formatted(name))
            }
        } else {
            e.reply("Der Name ist bereits vergeben!")
        }
    }
}