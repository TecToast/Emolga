package de.tectoast.emolga.commands.soullink

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class UpdateSoullinkCommand : Command("updatesoullink", "Updated die Message", CommandCategory.Soullink) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, 695943416789598208L)
    }

    override fun process(e: GuildCommandEvent) {
        updateSoullink()
        e.reply("Done!", true)
    }
}