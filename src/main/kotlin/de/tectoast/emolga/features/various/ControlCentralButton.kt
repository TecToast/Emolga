package de.tectoast.emolga.features.various

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.draft.Tierlist

object ControlCentralButton : ButtonFeature<ControlCentralButton.Args>(::Args, ButtonSpec("controlcentral")) {
    class Args : Arguments() {
        var mode by enumBasic<Mode>("mode", "mode")
    }

    enum class Mode {
        UPDATE_SLASH,
        UPDATE_TIERLIST,
        BREAKPOINT
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        var breakpoint = false
        deferReply(true)
        when (e.mode) {
            Mode.UPDATE_SLASH -> EmolgaMain.featureManager().updateFeatures(jda)
            Mode.UPDATE_TIERLIST -> Tierlist.setup()
            Mode.BREAKPOINT -> breakpoint = true
        }
        reply("Done!")
        if (breakpoint) {
            print("") // I have a JVM breakpoint here (as it turns out, a simple Unit gets optimized away)
        }
    }
}
