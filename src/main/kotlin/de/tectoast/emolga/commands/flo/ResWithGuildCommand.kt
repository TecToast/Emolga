package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.draft.EnterResult

class ResWithGuildCommand : Command("reswithguild", "Startet die interaktive Ergebniseingabe", CommandCategory.Flo) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("guild", "guild", "guild", ArgumentManagerTemplate.DiscordType.ID)
            add("user", "user", "user", ArgumentManagerTemplate.DiscordType.ID)
            add("opponent", "opponent", "opponent", ArgumentManagerTemplate.DiscordType.ID)
        }
        slash()
    }

    override suspend fun process(e: GuildCommandEvent) {
        EnterResult.handleStart(e)
    }

}
