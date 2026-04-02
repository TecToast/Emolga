package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.database.exposed.SpoilerTagsRepository
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.utils.dependency

object SpoilerTagsCommand : CommandFeature<NoArgs>(
    NoArgs(),
    CommandSpec(
        "spoilertags",
        K18n_SpoilerTags.Help,
    )
) {
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        if (dependency<SpoilerTagsRepository>().delete(iData.gid)) {
            return iData.reply(K18n_SpoilerTags.Disabled)
        }
        dependency<SpoilerTagsRepository>().insert(iData.gid)
        iData.reply(K18n_SpoilerTags.Enabled)
    }
}
