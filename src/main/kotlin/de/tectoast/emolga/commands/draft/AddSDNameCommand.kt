package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.database.exposed.SDInsertStatus
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.coroutines.await

object AddSDNameCommand : Command("addsdname", "Registriert deinen Showdown-Namen bei Emolga", CommandCategory.Draft) {
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
        val idSpecified = args.has("id")
        if (idSpecified && e.isNotFlo) {
            e.reply("Nur Flo darf den Command mit einer ID verwenden!")
            return
        }
        val name = args.getText("name")
        val id = if (idSpecified) args.getID("id") else e.author.idLong
        val specifiedName = if (idSpecified) e.jda.retrieveUserById(id).await().name else e.author.name
        when (SDNamesDB.addIfAbsent(name, id).await()) {
            SDInsertStatus.SUCCESS -> {
                if (idSpecified) {
                    e.reply("Der Name `$name` wurde für $specifiedName registriert!")
                } else {
                    e.reply("Der Name `$name` wurde für dich registriert!")
                }
            }

            SDInsertStatus.ALREADY_OWNED_BY_YOU -> {
                e.reply("Der Name `$name` gehört bereits ${if (idSpecified) specifiedName else "dir"}!")
            }

            SDInsertStatus.ALREADY_OWNED_BY_OTHER -> {
                e.reply("Der Name ist bereits von jemand anderem belegt! Es wird geprüft, ob dieser Name freigegeben werden kann.")
            }
        }
    }
}
