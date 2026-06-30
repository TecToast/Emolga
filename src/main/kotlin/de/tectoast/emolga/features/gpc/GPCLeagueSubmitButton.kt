package de.tectoast.emolga.features.gpc

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.koin.core.annotation.Single


@Single(binds = [ListenerProvider::class])
class GPCLeagueSubmitButton(private val modal: GPCLeagueSubmitModal) : ButtonFeature<GPCLeagueSubmitButton.Args>(
    ::Args,
    ButtonSpec("gpcleaguesubmit")
) {
    override val label = "Liga registrieren".k18n
    override val emoji = Emoji.fromUnicode("\uD83D\uDCE9")

    class Args : Arguments() {
        var catId by long().compIdOnly()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.replyModal(modal { this.catId = e.catId })
    }
}



