package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.TranslationsManager

class AddNickCommand : Command(
    "addnick",
    "Füge einen Nick für diese Sache hinzu, die ab dann damit abgerufen werden kann",
    CommandCategory.Pokemon
) {
    init {
        setCustomPermissions(PermissionPreset.CULT)
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("nick", "Nickname", "Der neue Nick für die Sache", ArgumentManagerTemplate.Text.any())
            .add(
                "stuff",
                "Sache",
                "Pokemon/Item/Whatever",
                Translation.Type.of(
                    Translation.Type.POKEMON,
                    Translation.Type.MOVE,
                    Translation.Type.ITEM,
                    Translation.Type.ABILITY
                )
            )
            .setExample("!addnick Banane Manectric")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val nickorig = args.getText("nick")
        val nick: String = nickorig.lowercase()
        val tr = getGerName(nick)
        if (tr.isSuccess && e.isNotFlo) {
            e.reply("**" + nickorig + "** ist bereits als **" + tr.translation + "** hinterlegt!")
            return
        }
        val t = args.getTranslation("stuff")
        TranslationsManager.addNick(nick, t)
        e.reply("**" + t.translation + "** kann ab jetzt auch mit **" + nickorig + "** abgefragt werden!")
        saveEmolgaJSON()
    }
}