package de.tectoast.emolga.features.dexquiz

import de.tectoast.emolga.domain.dexquiz.service.DexEntryShowService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class DexQuizHelpButton(private val service: DexEntryShowService) :
    ButtonFeature<DexQuizHelpButton.Args>(::Args, ButtonSpec("dexquiz_help")) {
    class Args : Arguments() {
        var id by int("id")
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.replyRaw(
            "Games: ${service.lookupDexEntryById(e.id)?.games?.joinToString() ?: "Unbekannt"}",
            ephemeral = true
        )
    }
}