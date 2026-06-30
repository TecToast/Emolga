package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.domain.game.service.EnglishResultsService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class EnglishResultsCommand(private val service: EnglishResultsService) : CommandFeature<NoArgs>(
    NoArgs(),
    CommandSpec(
        "englishresults",
        K18n_EnglishResults.Help,
    )
) {
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        return iData.reply(service.toggle(iData.gid))
    }
}
