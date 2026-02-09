package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.database.exposed.EnglishResultsDB
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs

object EnglishResultsCommand : CommandFeature<NoArgs>(
    NoArgs(),
    CommandSpec(
        "englishresults",
        K18n_EnglishResults.Help,
    )
) {
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        if (EnglishResultsDB.delete(iData.gid)) {
            return iData.reply(K18n_EnglishResults.German)
        }
        EnglishResultsDB.insert(iData.gid)
        iData.reply(K18n_EnglishResults.English)
    }
}
