package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class DelCommandCommand : Command("delcommand", "Deleted nen Command oder so", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "Name", "Der Name lol", ArgumentManagerTemplate.Text.any())
            .setExample("!delcommand lustigercommandname")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val name: String = e.arguments.getText("name").lowercase()
        val json = emolgaJSON.getJSONObject("customcommands")
        if (!json.has(name)) {
            e.reply("Dieser Command existiert nicht!")
            return
        }
        json.remove(name)
        e.reply("Der Command wurde erfolgreich gelöscht!")
        saveEmolgaJSON()
    }
}