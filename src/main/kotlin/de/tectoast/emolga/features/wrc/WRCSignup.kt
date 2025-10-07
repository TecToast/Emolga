package de.tectoast.emolga.features.wrc

import de.tectoast.emolga.database.exposed.WRCRunningDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData

object WRCSignup : ButtonFeature<WRCSignup.Args>(::Args, ButtonSpec("wrcsignup")) {
    class Args : Arguments() {
        var wrcname by string()
        var gameday by int()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val wrc = WRCRunningDB
    }
}