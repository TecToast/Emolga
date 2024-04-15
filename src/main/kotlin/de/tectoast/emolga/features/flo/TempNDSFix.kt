package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.MessageContextArgs
import de.tectoast.emolga.features.MessageContextFeature
import de.tectoast.emolga.features.MessageContextSpec
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.showdown.Analysis

object TempNDSFix : MessageContextFeature(MessageContextSpec("FloFixIt", Constants.G.NDS)) {
    context(InteractionData) override suspend fun exec(e: MessageContextArgs) {
        Analysis.analyseReplay(
            e.message.contentDisplay,
            resultchannelParam = jda.getTextChannelById(447357526997073932)!!,
            customGuild = gid
        )
        done(true)
    }
}
