package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.EnterResult

class ResultCommand :
    Command("result", "Startet die interaktive Ergebniseingabe", CommandCategory.Draft) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("opponent", "Gegner", "Dein Gegner in diesem Kampf", ArgumentManagerTemplate.DiscordType.USER)
        }
        slash(true, Constants.G.COMMUNITY, Constants.G.VIP)
    }

    override suspend fun process(e: GuildCommandEvent) {
        EnterResult.handleStart(e)
    }

}
