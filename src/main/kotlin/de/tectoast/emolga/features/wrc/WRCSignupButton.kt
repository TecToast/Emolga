package de.tectoast.emolga.features.wrc

import de.tectoast.emolga.database.exposed.WRCDataDB
import de.tectoast.emolga.database.exposed.WRCUserSignupDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.hasRole
import dev.minn.jda.ktx.messages.edit
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.components.buttons.ButtonStyle

object WRCSignupButton : ButtonFeature<WRCSignupButton.Args>(::Args, ButtonSpec("wrcsignup")) {
    override val buttonStyle = ButtonStyle.PRIMARY

    class Args : Arguments() {
        var wrcname by string()
        var gameday by int()
    }

    context(iData: InteractionData) override suspend fun exec(e: Args) {
        iData.ephemeralDefault()
        val name = e.wrcname
        val gameday = e.gameday
        val uid = iData.user
        val wrc = WRCDataDB.getByName(name) ?: return iData.reply("WRC nicht gefunden!")
        val isWarrior = iData.member().hasRole(wrc[WRCDataDB.WARRIORROLE])
        if (WRCUserSignupDB.unsignupUser(name, gameday, uid)) {
            iData.reply("Du hast dich für Spieltag $gameday erfolgreich von $name **ab**gemeldet!")
        } else {
            WRCUserSignupDB.signupUser(name, gameday, uid, isWarrior)
            iData.reply("Du hast dich für Spieltag $gameday erfolgreich bei $name **an**gemeldet!")
        }
        iData.message.edit(embeds = WRCUserSignupDB.buildSignupEmbed(name, gameday).into()).queue()
    }
}
