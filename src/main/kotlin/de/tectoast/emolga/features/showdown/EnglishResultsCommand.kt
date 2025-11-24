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
        "Stellt ein, ob die Pokemon in Ergebnissen auf deutsch oder englisch angezeigt werden (serverweit)",
    )
) {
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        if (EnglishResultsDB.delete(iData.gid)) {
            return iData.reply("Auf diesem Server werden die Pokemon bei Showdown-Ergebnissen nun **auf Deutsch angezeigt**!")
        }
        EnglishResultsDB.insert(iData.gid)
        iData.reply("Auf diesem Server werden die Pokemon bei Showdown-Ergebnissen nun **auf Englisch angezeigt**!")
    }
}
