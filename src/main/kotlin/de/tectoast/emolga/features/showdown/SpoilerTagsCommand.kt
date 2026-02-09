package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.database.exposed.SpoilerTagsDB
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs

object SpoilerTagsCommand : CommandFeature<NoArgs>(
    NoArgs(),
    CommandSpec(
        "spoilertags",
        K18n_SpoilerTags.Help,
    )
) {
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        if (SpoilerTagsDB.delete(iData.gid)) {
            return iData.reply(K18n_SpoilerTags.Disabled)
        }
        SpoilerTagsDB.insert(iData.gid)
        iData.reply(K18n_SpoilerTags.Enabled)
    }
}
