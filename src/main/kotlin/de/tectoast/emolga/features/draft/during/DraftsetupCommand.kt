package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db

object DraftsetupCommand : CommandFeature<DraftsetupCommand.Args>(
    ::Args,
    CommandSpec("draftsetup", "Startet das Draften der Liga in diesem Channel", *draftGuilds)
) {
    class Args : Arguments() {
        var name by string("Name", "Der Name der Liga")
        var switchdraft by boolean("switchdraft", "Ob es ein Switchdraft sein soll") {
            default = false
        }
    }

    init {
        restrict(members(Constants.HENNY))
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        db.league(e.name).startDraft(textChannel, fromFile = false, switchDraft = e.switchdraft)
        reply("+1", ephemeral = true)
    }
}
