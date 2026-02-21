package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.league.draft.generic.K18n_NoWritePermission
import de.tectoast.emolga.league.League
import net.dv8tion.jda.api.Permission

object DraftsetupCommand : CommandFeature<DraftsetupCommand.Args>(
    ::Args,
    CommandSpec("draftsetup", K18n_Draftsetup.Help)
) {
    class Args : Arguments() {
        var name by string("Name", K18n_Draftsetup.ArgName)
        var switchdraft by boolean("switchdraft", K18n_Draftsetup.ArgSwitchDraft) {
            default = false
        }
    }

    init {
        restrict(flo)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        League.executeOnFreshLock(e.name) {
            if (!iData.guild().selfMember.hasPermission(
                    iData.textChannel,
                    Permission.VIEW_CHANNEL,
                    Permission.MESSAGE_SEND
                )
            ) {
                return@executeOnFreshLock iData.reply(
                    K18n_NoWritePermission,
                    ephemeral = true
                )
            }
            startDraft(iData.textChannel, fromFile = false, switchDraft = e.switchdraft)
            iData.reply("+1", ephemeral = true)
        }
    }
}
