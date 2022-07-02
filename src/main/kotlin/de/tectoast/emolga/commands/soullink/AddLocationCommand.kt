package de.tectoast.emolga.commands.soullink

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class AddLocationCommand : Command("addlocation", "Fügt eine neue Location hinzu", CommandCategory.Soullink) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("location", "Location", "Die Location", ArgumentManagerTemplate.Text.any())
            .setExample("/addlocation Route 3")
            .build()
        slash(true, 695943416789598208L)
    }

    override fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val soullink = emolgaJSON.getJSONObject("soullink")
        val order = soullink.getStringList("order")
        val location = eachWordUpperCase(args!!.getText("location"))
        if (!order.contains(location)) {
            soullink.getJSONArray("order").put(location)
            e.reply("Die Location `%s` wurde eingetragen!".formatted(location))
            saveEmolgaJSON()
            updateSoullink()
            return
        }
        e.reply("Die Location gibt es bereits!")
    }
}