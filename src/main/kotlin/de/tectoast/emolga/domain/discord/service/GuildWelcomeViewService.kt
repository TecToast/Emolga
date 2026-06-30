package de.tectoast.emolga.domain.discord.service

import de.tectoast.emolga.domain.discord.model.AvailableLanguage
import de.tectoast.emolga.domain.discord.model.GuildWelcomeViewState
import de.tectoast.emolga.utils.BotConstants
import de.tectoast.emolga.utils.translateTo
import de.tectoast.generic.K18n_WelcomeMessage
import de.tectoast.k18n.generated.K18nLanguage
import org.koin.core.annotation.Single

@Single
class GuildWelcomeViewService(val botConstants: BotConstants) {
    fun buildWelcomeViewState(lang: K18nLanguage, owner: Long, guildName: String, gid: Long) = GuildWelcomeViewState(
        K18n_WelcomeMessage(owner, guildName, botConstants.botOwnerTag).translateTo(lang),
        lang,
        gid
    )

    fun getAvailableLanguages(displayLanguage: K18nLanguage): List<AvailableLanguage> =
        K18nLanguage.entries.map { AvailableLanguage(it.translateTo(displayLanguage), it.name) }
}
