package de.tectoast.emolga.commands.draft.during

import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db

object DraftsetupCommand : DraftCommand<DraftSetupCommandData>(
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
        setCustomPermissions(PermissionPreset.fromIDs(Constants.HENNY, 263729526436134934))
        slash(false, *draftGuilds)
    }

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = DraftSetupCommandData(
        e.arguments.getText("name"),
        e.arguments.getOrDefault("fromfile", false),
        e.arguments.getOrDefault("switchdraft", false)
    )

    context (DraftCommandData)
    override suspend fun exec(e: DraftSetupCommandData) {
        db.league(e.name).startDraft(
            textChannel,
            fromFile = e.fromFile,
            switchDraft = e.switchDraft
        )
        reply("+1", ephemeral = true)
    }
}

class DraftSetupCommandData(
    val name: String,
    val fromFile: Boolean,
    val switchDraft: Boolean
) : SpecifiedDraftCommandData
