package de.tectoast.emolga.features.draft

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions

object StartMenschenhandel : CommandFeature<StartMenschenhandel.Args>(
    ::Args,
    CommandSpec("startmenschenhandel", "Startet die beste Sache einer Coach-Season")
) {
    class Args : Arguments() {
        var channel by textchannel("Channel", "Der Channel lol")
    }

    override val slashPermissions = DefaultMemberPermissions.DISABLED
    override val check: suspend InteractionData.() -> Boolean = { member().isOwner }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        val tc = e.channel
        db.aslcoach.only().apply {
            textChannel = tc.idLong
            tc.sendMessage("Möge der Menschenhandel beginnen!").queue()
            nextCoach()
            save()
        }
    }
}
