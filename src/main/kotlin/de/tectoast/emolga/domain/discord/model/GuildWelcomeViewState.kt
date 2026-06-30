package de.tectoast.emolga.domain.discord.model

import de.tectoast.k18n.generated.K18nLanguage

data class GuildWelcomeViewState(val messageContent: String, val selectedLanguage: K18nLanguage, val guild: Long)