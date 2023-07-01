package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.db

class DraftsetupCommand :
    Command("draftsetup", "Startet das Draften der Liga in diesem Channel (nur Flo)", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "Name", "Der Name der Liga/des Drafts", ArgumentManagerTemplate.Text.any())
            .add(
                "tc",
                "Channel",
                "Der Channel, wo die Teamübersicht sein soll",
                ArgumentManagerTemplate.DiscordType.CHANNEL,
                true
            )
            .add(
                "fromfile",
                "Datei",
                "Ob alte Daten verwendet werden sollen",
                ArgumentManagerTemplate.ArgumentBoolean,
                optional = true
            )
            .add(
                "switchdraft",
                "Switchdraft",
                "Ob dieser Draft ein Switchdraft ist",
                ArgumentManagerTemplate.ArgumentBoolean,
                optional = true
            )
            .build()
        setCustomPermissions(PermissionPreset.fromIDs(297010892678234114, 263729526436134934))
        slash(false, *draftGuilds)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        db.league(args.getText("name")).startDraft(
            e.textChannel,
            fromFile = args.getOrDefault("fromfile", false),
            switchDraft = args.getOrDefault("switchdraft", false)
        )
        e.reply("+1", ephemeral = true)
    }
}
