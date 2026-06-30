package de.tectoast.emolga.utils

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.generic.K18n_English
import de.tectoast.generic.K18n_German
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import de.tectoast.k18n.generated.K18nLanguage
import de.tectoast.k18n.generated.K18nMessage
import net.dv8tion.jda.api.interactions.DiscordLocale

fun K18nMessage.default() = translateTo(K18N_DEFAULT_LANGUAGE)

context(iData: InteractionData)
fun K18nMessage.t() = translateTo(iData.language)
fun K18nLanguage.toDiscordLocale() = when (this) {
    K18nLanguage.DE -> DiscordLocale.GERMAN
    K18nLanguage.EN -> DiscordLocale.ENGLISH_US
}

fun K18nLanguage.translateTo(language: K18nLanguage) = when (this) {
    K18nLanguage.DE -> K18n_German
    K18nLanguage.EN -> K18n_English
}.translateTo(language)

fun DiscordLocale.toK18nLanguage() = when (this) {
    DiscordLocale.GERMAN -> K18nLanguage.DE
    else -> K18nLanguage.EN
}

fun K18nMessage.toDiscordLocaleMap() = K18nLanguage.entries.associate { lang ->
    lang.toDiscordLocale() to translateTo(lang)
}

@JvmInline
value class SimpleK18nMessage(val msg: String) : K18nMessage {
    override fun translateTo(language: K18nLanguage): String = msg
}

inline val String.k18n: K18nMessage get() = SimpleK18nMessage(this)

data object EmptyMessage : K18nMessage {
    override fun translateTo(language: K18nLanguage): String = ""
}

data class MappedMessage(private val original: K18nMessage, private val transformer: (String) -> String) : K18nMessage {
    override fun translateTo(language: K18nLanguage): String = transformer(original.translateTo(language))
}

context(language: K18nLanguage)
operator fun K18nMessage.invoke() = translateTo(language)
inline fun b(crossinline builder: context(K18nLanguage) () -> String) = object : K18nMessage {
    override fun translateTo(language: K18nLanguage): String = with(language) { builder() }
}