package de.tectoast.emolga.domain.league.signup.model

import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.interactions.components.TextInputDefaults

data class ModalInputOptions(
    val label: K18nMessage,
    val required: Boolean,
    val description: K18nMessage? = null,
    val placeholder: K18nMessage? = null,
    val requiredLength: IntRange? = TextInputDefaults.requiredLength,
    val list: List<String>? = null
)