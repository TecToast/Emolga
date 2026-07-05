package de.tectoast.emolga.domain.config.model

import de.tectoast.k18n.generated.K18nLanguage

sealed interface GuildConfigType<T> {
    val default: T

    data object SpoilerTags : GuildConfigType<Boolean> {
        override val default = false
    }

    data object EnglishResults : GuildConfigType<Boolean> {
        override val default = false
    }

    data object EmbedResults : GuildConfigType<Boolean> {
        override val default = true
    }

    data object Language : GuildConfigType<K18nLanguage> {
        override val default = K18nLanguage.DE
    }

}