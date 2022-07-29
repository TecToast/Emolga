package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.jsolf.JSONObject
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
    override fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val cmdname: String = args.getText("cmdname").lowercase()
        val o = emolgaJSON.getJSONObject("customcommands")
        if (o.has(cmdname) || byName(cmdname) != null) {
            e.reply("Es existiert bereits ein !$cmdname Command!")
            return
        }
        val m = e.message!!
        val json = JSONObject()
        json.put("text", false)
        json.put("image", false)
        var file: File? = null
        if (m.attachments.size > 0) {
            val a = m.attachments[0]
            file = a.proxy.downloadToFile(File("customcommandimages/" + a.fileName)).get()
            json.put("image", file.absolutePath)
        }
        if (!args.has("text")) {
            if (file == null) {
                e.reply("Du musst entweder einen Text oder ein Bild angeben!")
                return
            }
        } else {
            json.put("text", args.getText("text"))
        }
        o.put(cmdname, json)
        saveEmolgaJSON()
        e.reply("Der Command wurde erfolgreich registriert!")
    }
}