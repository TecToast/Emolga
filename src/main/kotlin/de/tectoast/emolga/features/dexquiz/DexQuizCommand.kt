package de.tectoast.emolga.features.dexquiz

import de.tectoast.emolga.domain.dexquiz.service.DexEntryShowService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class DexQuizCommand(
    private val service: DexEntryShowService,
    private val solutionButton: DexQuizSolutionButton,
    private val helpButton: DexQuizHelpButton
) :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("dexquiz", "Postet einen zufälligen Dex-Eintrag.".k18n)) {
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        service.generateNewDexEntry()?.let { entry ->
            iData.replyRaw(
                "```${entry.entry}```",
                components = listOf(
                    helpButton("Games".k18n, ButtonStyle.PRIMARY) {
                        this.id = entry.id
                    },
                    solutionButton("Lösung".k18n, ButtonStyle.SUCCESS) {
                        this.id = entry.id
                    },
                ).into()
            )
        } ?: iData.replyRaw("Es konnte kein Dex-Eintrag generiert werden.")
    }
}