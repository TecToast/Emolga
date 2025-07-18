package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only

object StartMenschenhandel : CommandFeature<StartMenschenhandel.Args>(
    ::Args,
    CommandSpec("startmenschenhandel", "Startet die beste Sache einer Coach-Season")
) {
    class Args : Arguments() {
        var channel by long("Channel", "Der Channel lol")
    }

    init {
        slashPrivate()
        restrict { member().isOwner }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val tcid = e.channel
        iData.done(true)
        db.aslcoach.only().apply {
            textChannel = tcid
            iData.jda.getTextChannelById(tcid)!!.sendMessage("Möge der Menschenhandel beginnen!").queue()
            nextCoach()
            save()
        }
    }
}
