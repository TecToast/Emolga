package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants

object LogoForCommand : Command("logofor", "Reicht ein Logo für jemanden ein", CommandCategory.Draft) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("user", "User", "Der User", ArgumentManagerTemplate.DiscordType.USER)
            add("logo", "Logo", "Das Logo", ArgumentManagerTemplate.DiscordFile("*"))
        }
        slash(true, Constants.G.FLP, Constants.G.ASL, Constants.G.NDS)
        setCustomPermissions(PermissionPreset.fromIDs(242427180942884864))
    }

    override suspend fun process(e: GuildCommandEvent) {
        LogoCommand.insertLogo(e, e.arguments.getMember("user").idLong)
    }

}
