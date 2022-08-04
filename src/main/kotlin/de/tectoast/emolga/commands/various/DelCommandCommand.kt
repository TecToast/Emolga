package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.Emolga

class DelCommandCommand : Command("delcommand", "Deleted nen Command oder so", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "Name", "Der Name lol", ArgumentManagerTemplate.Text.any())
            .setExample("!delcommand lustigercommandname")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val name: String = e.arguments.getText("name").lowercase()
        val json = Emolga.get.customcommands
        if (name !in json) {
            e.reply("Dieser Command existiert nicht!")
            return
        }
        json.remove(name)
        e.reply("Der Command wurde erfolgreich gelöscht!")
        saveEmolgaJSON()
    }
}