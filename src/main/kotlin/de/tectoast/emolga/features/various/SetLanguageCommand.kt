package de.tectoast.emolga.features.various

import de.tectoast.emolga.domain.language.repository.GuildLanguageRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.k18n.generated.K18nLanguage
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SetLanguageCommand(private val languageRepo: GuildLanguageRepository) :
    CommandFeature<SetLanguageCommand.Args>(::Args, CommandSpec("setlanguage", K18n_SetLanguage.Help)) {
    class Args : Arguments() {
        var language by enumBasic<K18nLanguage>("language", K18n_SetLanguage.ArgLanguage)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        languageRepo.setLanguage(iData.gid, e.language)
        iData.replyRaw(K18n_SetLanguage.Success(e.language.name).translateTo(e.language))
    }
}
