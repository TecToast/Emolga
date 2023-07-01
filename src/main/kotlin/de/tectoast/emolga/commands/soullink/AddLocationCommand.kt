package de.tectoast.emolga.commands.soullink

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only

class AddLocationCommand : Command("addlocation", "Fügt eine neue Location hinzu", CommandCategory.Soullink) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("location", "Location", "Die Location", ArgumentManagerTemplate.Text.any())
            .setExample("/addlocation Route 3")
            .build()
        slash(true, 695943416789598208L)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val soullink = db.soullink.only()
        val order = soullink.order
        val location = eachWordUpperCase(args.getText("location"))
        if (!order.contains(location)) {
            order.add(location)
            e.reply("Die Location `$location` wurde eingetragen!")
            soullink.save()
            updateSoullink()
            return
        }
        e.reply("Die Location gibt es bereits!")
    }
}
