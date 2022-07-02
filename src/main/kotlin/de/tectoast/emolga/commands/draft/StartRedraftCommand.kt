package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.draft.Draft

class StartRedraftCommand : Command("startredraft", "Startet einen Redraft", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "Name", "Der Name der Liga/des Drafts", ArgumentManagerTemplate.Text.any())
            .add(
                "file",
                "FromFile",
                "Obs von der File geladen werden soll `file`",
                ArgumentManagerTemplate.Text.any(),
                true
            )
            .setExample("!startredraft Emolga-Conference")
            .build()
        setCustomPermissions(PermissionPreset.fromIDs(297010892678234114L, 280825853401628674L))
    }

    override fun process(e: GuildCommandEvent) {
        val args = e.arguments!!
        val fromFile = args.getOrDefault("file", "").equals("file", ignoreCase = true)
        Draft(e.textChannel, args.getText("name"), null, fromFile, true)
        if (fromFile) e.message!!.delete().queue()
    }
}