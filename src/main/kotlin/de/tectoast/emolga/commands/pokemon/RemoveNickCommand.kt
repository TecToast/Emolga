package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.TranslationsManager

class RemoveNickCommand : Command("removenick", "Removed den Nick", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("nick", "Nickname", "Der Nickname, der deleted werden soll", ArgumentManagerTemplate.Text.any())
            .setExample("!removenick Schmutz")
            .build()
        setCustomPermissions(PermissionPreset.fromIDs(452575044070277122L, 535095576136515605L))
    }

    override fun process(e: GuildCommandEvent) {
        val args = e.arguments!!
        val nick = args.getText("nick")
        if (TranslationsManager.removeNick(nick)) e.reply("Dieser Nickname wurde entfernt!") else e.reply("Dieser Nickname existiert nicht!")
    }
}