package de.tectoast.emolga.commands.draft.during

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db

object DraftsetupCommand : TestableCommand<DraftSetupCommandArgs>(
    "draftsetup",
    "Startet das Draften der Liga in diesem Channel (nur Flo)",
    CommandCategory.Flo
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "Name", "Der Name der Liga/des Drafts", ArgumentManagerTemplate.Text.any())
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
        setCustomPermissions(PermissionPreset.fromIDs(Constants.HENNY, 263729526436134934, Constants.INK))
        slash(false, *draftGuilds)
    }

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = DraftSetupCommandArgs(
        e.arguments.getText("name"),
        e.arguments.getOrDefault("fromfile", false),
        e.arguments.getOrDefault("switchdraft", false)
    )

    context (CommandData)
    override suspend fun exec(e: DraftSetupCommandArgs) {
        db.league(e.name).startDraft(
            textChannel,
            fromFile = e.fromFile,
            switchDraft = e.switchDraft
        )
        reply("+1", ephemeral = true)
    }
}

class DraftSetupCommandArgs(
    val name: String,
    val fromFile: Boolean,
    val switchDraft: Boolean
) : CommandArgs
