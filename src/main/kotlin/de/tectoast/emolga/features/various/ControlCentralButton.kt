package de.tectoast.emolga.features.various

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.logging.LogConfigReload
import de.tectoast.emolga.utils.draft.Tierlist

object ControlCentralButton : ButtonFeature<ControlCentralButton.Args>(::Args, ButtonSpec("controlcentral")) {
    class Args : Arguments() {
        var mode by enumBasic<Mode>()
    }

    enum class Mode {
        UPDATE_TIERLIST,
        BREAKPOINT,
        RELOAD_LOG_CONFIG
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        var breakpoint = false
        iData.deferReply(true)
        when (e.mode) {
            Mode.UPDATE_TIERLIST -> Tierlist.setup()
            Mode.BREAKPOINT -> breakpoint = true
            Mode.RELOAD_LOG_CONFIG -> LogConfigReload.reloadConfiguration()
        }
        iData.reply("Done!")
        if (breakpoint) {
            print("") // I have a JVM breakpoint here (as it turns out, a simple Unit gets optimized away)
        }
    }
}
