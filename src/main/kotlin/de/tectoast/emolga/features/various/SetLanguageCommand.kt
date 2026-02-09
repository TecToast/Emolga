package de.tectoast.emolga.features.various

import de.tectoast.emolga.database.exposed.GuildLanguageDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.k18n.generated.K18nLanguage

object SetLanguageCommand :
    CommandFeature<SetLanguageCommand.Args>(::Args, CommandSpec("setlanguage", K18n_SetLanguage.Help)) {
    class Args : Arguments() {
        var language by enumBasic<K18nLanguage>("language", K18n_SetLanguage.ArgLanguage)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        GuildLanguageDB.setLanguage(iData.gid, e.language)
        iData.reply(K18n_SetLanguage.Success(e.language.name).translateTo(e.language))
    }
}