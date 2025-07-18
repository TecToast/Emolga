package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.Constants

object DraftsetupCommand : CommandFeature<DraftsetupCommand.Args>(
    ::Args,
    CommandSpec("draftsetup", "Startet das Draften der Liga in diesem Channel", *draftGuilds)
) {
    class Args : Arguments() {
        var name by string("Name", "Der Name der Liga")
        var switchdraft by boolean("switchdraft", "Ob es ein Switchdraft sein soll") {
            default = false
        }
        var nameguild by long("NameGuild", "Der Server, von dem die Namen geholt werden sollen").nullable()
    }

    init {
        restrict(members(Constants.M.HENNY, Constants.M.INK, 265917650490753025))
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        League.executeOnFreshLock(e.name) {
            startDraft(iData.textChannel, fromFile = false, switchDraft = e.switchdraft, nameGuildId = e.nameguild)
            iData.reply("+1", ephemeral = true)
        }
    }
}
