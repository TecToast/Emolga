package de.tectoast.emolga.commands.soullink

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.json.Emolga

class StatusCommand : Command("status", "Setzt den Status eines Encounters", CommandCategory.Soullink) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("location", "Location", "Die Location", ArgumentManagerTemplate.Text.any())
            .add(
                "status",
                "Status",
                "Der Status",
                ArgumentManagerTemplate.Text.of(SubCommand.of("Team"), SubCommand.of("Box"), SubCommand.of("RIP"))
            )
            .setExample("/status Route 1 RIP")
            .build()
        slash(true, 695943416789598208L)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val soullink = Emolga.get.soullink
        val args = e.arguments
        val location = eachWordUpperCase(args.getText("location"))
        if (location !in soullink.order) {
            e.reply("Diese Location ist derzeit nicht im System!")
            return
        }
        soullink.mons[location]!!["status"] = args.getText("status")
        e.reply("\uD83D\uDC4D")
        saveEmolgaJSON()
        updateSoullink()
    }
}