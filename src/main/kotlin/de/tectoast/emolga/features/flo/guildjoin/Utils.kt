package de.tectoast.emolga.features.flo.guildjoin

import de.tectoast.emolga.domain.discord.model.GuildWelcomeViewState
import de.tectoast.emolga.domain.discord.service.GuildWelcomeViewService
import de.tectoast.generic.K18n_SelectLanguage
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.into

internal fun GuildWelcomeViewService.buildWelcomeMessageMenu(viewState: GuildWelcomeViewState, menu: LanguageMenu) =
    menu(
        placeholder = K18n_SelectLanguage.translateTo(viewState.selectedLanguage),
        options = getAvailableLanguages(viewState.selectedLanguage).map {
            SelectOption(
                it.displayName,
                it.internalId
            )
        }
    ) {
        this.guild = viewState.guild
    }.into()