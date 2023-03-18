package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.emolga.draft.ITP
import de.tectoast.emolga.utils.json.emolga.draft.League

class PickTypeCommand : Command("picktype", "Pickt einen Tera-Typen", CommandCategory.Draft) {

    companion object {
        val typeList = setOf(
            "Normal",
            "Feuer",
            "Wasser",
            "Pflanze",
            "Gestein",
            "Boden",
            "Geist",
            "Unlicht",
            "Drache",
            "Fee",
            "Eis",
            "Kampf",
            "Elektro",
            "Flug",
            "Gift",
            "Psycho",
            "Stahl",
            "Käfer"
        )
    }

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add(
                "type",
                "Typ",
                "Der Typ, der gewählt werden soll",
                ArgumentManagerTemplate.Text.of(
                    typeList.map { SubCommand(it, it) }
                ))
        }
        slash(false, 651152835425075218)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val d =
            League.byCommand(e) ?: return e.reply("Es läuft zurzeit kein Draft in diesem Channel!", ephemeral = true)
        if (d !is ITP) {
            e.reply("Dieser Befehl funktioniert nur im ITP Draft!")
            return
        }
        if (d.current in d.teraTypes) {
            e.reply("Du hast bereits einen Tera-Typen gepickt!")
            return
        }
        val type = e.arguments.getText("type")
        if (type !in typeList) {
            e.reply("Dieser Typ existiert nicht!")
            return
        }
        if (type in d.teraTypes.values) {
            e.reply("Dieser Typ wurde bereits gepickt!")
            return
        }
        d.teraTypes[d.current] = type
        d.replyGeneral(e, "den Tera-Typ `$type` gepickt!")
        d.typeDoc(type)
        d.afterPickOfficial()
    }
}
