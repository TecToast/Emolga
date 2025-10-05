package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import net.dv8tion.jda.api.Permission

object DraftsetupCommand : CommandFeature<DraftsetupCommand.Args>(
    ::Args,
    CommandSpec("draftsetup", "Startet das Draften der Liga in diesem Channel")
) {
    class Args : Arguments() {
        var name by string("Name", "Der Name der Liga")
        var switchdraft by boolean("switchdraft", "Ob es ein Switchdraft sein soll") {
            default = false
        }
        var nameguild by long("NameGuild", "Der Server, von dem die Namen geholt werden sollen").nullable()
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
                    "Ich habe keine Schreibrechte in diesem Channel!",
                    ephemeral = true
                )
            }
            startDraft(iData.textChannel, fromFile = false, switchDraft = e.switchdraft, nameGuildId = e.nameguild)
            iData.reply("+1", ephemeral = true)
        }
    }
}
