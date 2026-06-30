package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.domain.game.service.SpoilerTagsService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SpoilerTagsCommand(private val service: SpoilerTagsService) : CommandFeature<NoArgs>(
    NoArgs(),
    CommandSpec(
        "spoilertags",
        K18n_SpoilerTags.Help,
    )
) {
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        return iData.reply(if (service.toggle(iData.gid)) K18n_SpoilerTags.Enabled else K18n_SpoilerTags.Disabled)
    }
}
