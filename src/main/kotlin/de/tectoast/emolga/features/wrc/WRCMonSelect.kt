package de.tectoast.emolga.features.wrc

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.SelectMenuFeature
import de.tectoast.emolga.features.SelectMenuSpec
import de.tectoast.emolga.features.wrc.WRCMonSelect.Args

object WRCMonSelect : SelectMenuFeature<Args>(::Args, SelectMenuSpec("wrcmonselect")) {
    class Args : Arguments() {
        val wrcname by string().compIdOnly()
        val gameday by int().compIdOnly()
        val selection by multiOption(2..2)
    }

    context(data: InteractionData)
    override suspend fun exec(e: Args) {
        // TODO
    }
}
