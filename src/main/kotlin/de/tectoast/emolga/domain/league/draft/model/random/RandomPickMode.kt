package de.tectoast.emolga.domain.league.draft.model.random

import de.tectoast.emolga.features.system.model.ArgumentPresence
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface RandomPickMode {
    fun provideCommandOptions(): Map<RandomPickArgument, ArgumentPresence>

    @Serializable
    @SerialName("Default")
    data class Default(val tierRequired: Boolean = false, val typeAllowed: Boolean = true) : RandomPickMode {
        override fun provideCommandOptions(): Map<RandomPickArgument, ArgumentPresence> {
            return buildMap {
                put(RandomPickArgument.TIER, if (tierRequired) ArgumentPresence.REQUIRED else ArgumentPresence.OPTIONAL)
                if (typeAllowed) put(RandomPickArgument.TYPE, ArgumentPresence.OPTIONAL)
            }
        }
    }

    @Serializable
    @SerialName("TypeTierlist")
    data object TypeTierlist : RandomPickMode {
        override fun provideCommandOptions(): Map<RandomPickArgument, ArgumentPresence> {
            return mapOf(RandomPickArgument.TYPE to ArgumentPresence.REQUIRED)
        }
    }
}
