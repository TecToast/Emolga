package de.tectoast.emolga.features.system.types

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.model.ArgBuilder
import de.tectoast.emolga.utils.k18n
import de.tectoast.emolga.utils.t
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import de.tectoast.k18n.generated.K18nLanguage
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.interactions.components.button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

abstract class ButtonFeature<A : Arguments>(argsFun: () -> A, spec: ButtonSpec) :
    Feature<ButtonSpec, ButtonInteractionEvent, A>(argsFun, spec, ButtonInteractionEvent::class, eventToName) {
    open val buttonStyle = ButtonStyle.PRIMARY
    open val label: K18nMessage = spec.name.k18n
    open val emoji: Emoji? = null
    override suspend fun populateArgs(data: InteractionData, e: ButtonInteractionEvent, args: A) {
        val argsFromEvent = e.componentId.substringAfter(";").split(";")
        for ((index, arg) in args.args.withIndex()) {
            val m = argsFromEvent.getOrNull(index)?.takeIf { it.isNotBlank() }
            if (m != null) arg.parse(data, m)
        }
    }

    context(iData: InteractionData)
    operator fun invoke(
        label: K18nMessage = this.label,
        buttonStyle: ButtonStyle = this.buttonStyle,
        emoji: Emoji? = this.emoji,
        disabled: Boolean = false,
        argsBuilder: ArgBuilder<A> = {}
    ) = button(createComponentId(argsBuilder), label.t(), style = buttonStyle, emoji = emoji, disabled = disabled)

    fun withoutIData(
        language: K18nLanguage = K18N_DEFAULT_LANGUAGE,
        label: K18nMessage = this.label,
        buttonStyle: ButtonStyle = this.buttonStyle,
        emoji: Emoji? = this.emoji,
        disabled: Boolean = false,
        argsBuilder: ArgBuilder<A> = {}
    ) = button(
        createComponentId(argsBuilder),
        label.translateTo(language),
        style = buttonStyle,
        emoji = emoji,
        disabled = disabled
    )


    companion object {
        val eventToName: (ButtonInteractionEvent) -> String = { it.componentId.substringBefore(";") }
    }
}