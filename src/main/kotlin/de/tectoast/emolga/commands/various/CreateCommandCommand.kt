package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.json.emolga.customcommand.CCData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CreateCommandCommand : Command("createcommand", "Erstellt einen Command", CommandCategory.Various) {
    init {
        setCustomPermissions(PermissionPreset.CULT)
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("cmdname", "Command-Name", "Der Command-Name", ArgumentManagerTemplate.Text.any())
            .add(
                "text",
                "Text",
                "Der Text, der dann geschickt werden soll (kann leer sein, wenn man ein Bild anhängt)",
                ArgumentManagerTemplate.Text.any(),
                true
            )
            .setExample("!createcommand nein Tom schreit ein kraftvolles **NEIN!** in die Runde!")
            .build()
    }

    @Throws(Exception::class)
    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val cmdname: String = args.getText("cmdname").lowercase()
        val o = Emolga.get.customcommands
        if (cmdname in o || byName(cmdname) != null) {
            e.reply("Es existiert bereits ein !$cmdname Command!")
            return
        }
        val m = e.message!!
        var file: File? = null
        val cc = CCData()
        if (m.attachments.size > 0) {
            val a = m.attachments[0]
            file = withContext(Dispatchers.IO) {
                a.proxy.downloadToFile(File("customcommandimages/" + a.fileName)).get()
            }
            cc.image = file.absolutePath
        }
        if (!args.has("text")) {
            if (file == null) {
                e.reply("Du musst entweder einen Text oder ein Bild angeben!")
                return
            }
        } else {
            cc.text = args.getText("text")
        }
        o[cmdname] = cc
        saveEmolgaJSON()
        e.reply("Der Command wurde erfolgreich registriert!")
    }
}