package de.tectoast.emolga.features.flo.guildjoin

import de.tectoast.emolga.discord.GuildMetaRepository
import de.tectoast.emolga.domain.discord.service.GuildWelcomeViewService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.SelectMenuSpec
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.system.types.StringSelectMenuFeature
import de.tectoast.k18n.generated.K18nLanguage
import org.koin.core.annotation.Single


@Single(binds = [ListenerProvider::class])
class LanguageMenu(private val service: GuildWelcomeViewService, private val guildMetaRepo: GuildMetaRepository) :
    StringSelectMenuFeature<LanguageMenu.Args>(::Args, SelectMenuSpec("languagemenu")) {

    class Args : Arguments() {
        var selection by singleOption()
        var guild by long().compIdOnly()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val lang = enumValueOf<K18nLanguage>(e.selection)
        val guildName = guildMetaRepo.getGuildName(e.guild) ?: "_Unknown_"
        val viewState = service.buildWelcomeViewState(lang, iData.user, guildName, e.guild)
        val menu = service.buildWelcomeMessageMenu(viewState, menu = this)
        iData.editRaw(content = viewState.messageContent, components = menu)
    }
}