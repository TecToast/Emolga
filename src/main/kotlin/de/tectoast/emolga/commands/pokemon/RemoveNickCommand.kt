package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.database.exposed.TranslationsDB

class RemoveNickCommand : Command("removenick", "Removed den Nick", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("nick", "Nickname", "Der Nickname, der deleted werden soll", ArgumentManagerTemplate.Text.any())
            .setExample("!removenick Schmutz").build()
        setCustomPermissions(PermissionPreset.fromIDs(452575044070277122L, 535095576136515605L))
    }

    override suspend fun process(e: GuildCommandEvent) =
        e.reply(if (TranslationsDB.removeNick(e.arguments.getText("nick"))) "Dieser Nickname wurde entfernt!" else "Dieser Nickname existiert nicht!")
}
