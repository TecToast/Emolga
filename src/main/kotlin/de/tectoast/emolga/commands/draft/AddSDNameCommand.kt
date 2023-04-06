package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.utils.Constants

class AddSDNameCommand : Command("addsdname", "Registriert deinen Showdown-Namen bei Emolga", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "SD-Name", "Der SD-Name", ArgumentManagerTemplate.Text.any())
            .add("id", "Die ID (nur Flo)", "Nur für Flo", ArgumentManagerTemplate.DiscordType.ID, true)
            .setExample("!addsdname TecToast")
            .build()
        slash(false, Constants.G.GILDE)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        if (args.has("id") && e.isNotFlo) {
            e.reply("Nur Flo darf den Command mit einer ID verwenden!")
            return
        }
        val name = args.getText("name")
        val id = if (args.has("id")) args.getID("id") else e.author.idLong
        if (SDNamesDB.addIfAbsent(name, id)) {
            if (args.has("id")) {
                e.jda.retrieveUserById(id)
                    .queue { e.reply("Der Name `$name` wurde für ${it.name} registriert!") }
            } else {
                e.reply("Der Name `$name` wurde für dich registriert!")
            }
        } else {
            e.reply("Der Name ist bereits vergeben!")
        }
    }
}
