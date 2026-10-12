package de.tectoast.emolga.features.dexquiz

import de.tectoast.emolga.domain.dexquiz.service.DexEntryShowService
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.Language
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class DexQuizSolutionButton(
    private val service: DexEntryShowService,
    private val displayService: PokemonDisplayService
) : ButtonFeature<DexQuizSolutionButton.Args>(::Args, ButtonSpec("dexquiz_solution")) {
    class Args : Arguments() {
        var id by int("id")
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.replyRaw(
            "Lösung: ${
                service.lookupDexEntryById(e.id)?.showdownId?.let {
                    displayService.getDisplayName(
                        it, 0,
                        Language.GERMAN, withAdditionalEnglish = true
                    )
                } ?: "Unbekannt"
            }", ephemeral = true)
    }
}