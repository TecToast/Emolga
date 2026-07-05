package de.tectoast.emolga.features.various

import de.tectoast.emolga.domain.game.service.EmbedResultsService
import de.tectoast.emolga.domain.game.service.EnglishResultsService
import de.tectoast.emolga.domain.game.service.SpoilerTagsService
import de.tectoast.emolga.domain.language.repository.GuildLanguageRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.various.ConfigCommand.SetLanguage.Args
import de.tectoast.emolga.features.various.config.*
import de.tectoast.k18n.generated.K18nLanguage
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class ConfigCommand(spoilerTags: SpoilerTags, englishResults: EnglishResults, setLanguage: SetLanguage, embedResults: EmbedResults) : CommandFeature<NoArgs>(NoArgs(), CommandSpec("config", K18n_GenericHelp)) {

    override val children = listOf(spoilerTags, englishResults, setLanguage, embedResults)

    @Single
    class SpoilerTags(private val service: SpoilerTagsService) : CommandFeature<NoArgs>(NoArgs(), CommandSpec("spoilertags", K18n_SpoilerTags.Help)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            return iData.reply(if (service.toggle(iData.gid)) K18n_SpoilerTags.Enabled else K18n_SpoilerTags.Disabled)
        }
    }

    @Single
    class EnglishResults(private val service: EnglishResultsService) : CommandFeature<NoArgs>(NoArgs(), CommandSpec("englishresults", K18n_EnglishResults.Help)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            return iData.reply(service.toggle(iData.gid))
        }
    }

    @Single
    class SetLanguage(private val languageRepo: GuildLanguageRepository) : CommandFeature<Args>(::Args, CommandSpec("setlanguage", K18n_SetLanguage.Help)) {

        class Args : Arguments() {
            var language by enumBasic<K18nLanguage>("language", K18n_SetLanguage.ArgLanguage)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            languageRepo.setLanguage(iData.gid, e.language)
            iData.replyRaw(K18n_SetLanguage.Success(e.language.name).translateTo(e.language))
        }
    }

    @Single
    class EmbedResults(private val service: EmbedResultsService) : CommandFeature<NoArgs>(NoArgs(), CommandSpec("embedresults", K18n_EmbedResults.Help)) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            return iData.reply(if(service.toggle(iData.gid)) K18n_EmbedResults.Enabled else K18n_EmbedResults.Disabled)
        }
    }



    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {

    }

}