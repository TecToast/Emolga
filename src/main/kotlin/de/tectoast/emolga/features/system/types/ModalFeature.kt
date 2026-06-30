package de.tectoast.emolga.features.system.types

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.ArgSpec
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ModalSpec
import de.tectoast.emolga.features.system.model.ArgBuilder
import de.tectoast.emolga.features.system.nameToDiscordOption
import de.tectoast.emolga.utils.t
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.interactions.components.InlineTextInput
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.TextInput
import net.dv8tion.jda.api.components.Component
import net.dv8tion.jda.api.components.label.LabelChildComponent
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.modals.Modal

abstract class ModalFeature<A : Arguments>(argsFun: () -> A, spec: ModalSpec) :
    Feature<ModalSpec, ModalInteractionEvent, A>(
        argsFun, spec, ModalInteractionEvent::class, eventToName
    ) {

    open val title: K18nMessage
        get() = throw NotImplementedError("Title not implemented for modal ${this.spec.name}")

    override suspend fun populateArgs(data: InteractionData, e: ModalInteractionEvent, args: A) {
        val (compId, regular) = args.args.partition { it.compIdOnly }
        val argsFromEvent = e.modalId.substringAfter(";").split(";")
        populateArgs(data, compId) { _, index ->
            argsFromEvent.getOrNull(index)?.takeIf { it.isNotBlank() }
        }
        populateArgs(data, regular) { name, _ ->
            val value = e.getValue(name) ?: return@populateArgs null
            when (value.type) {
                Component.Type.STRING_SELECT -> value.asStringList
                Component.Type.USER_SELECT -> value.asLongList
                Component.Type.TEXT_INPUT -> value.asString.takeIf { it.isNotBlank() }
                else -> null
            }
        }
    }

    context(iData: InteractionData)
    suspend operator fun invoke(
        title: K18nMessage = this.title,
        specificallyEnabledArgs: Map<ModalKey, Boolean> = emptyMap(),
        argsBuilder: ArgBuilder<A> = {}
    ): Modal {
        val modalEntries = argsFun().apply(argsBuilder).args.mapNotNull { arg ->
            if (arg.compIdOnly) return@mapNotNull null
            val spec = arg.spec as? ModalArgSpec
            spec?.modalEnableKey?.let { key ->
                if (specificallyEnabledArgs[key] != true) return@mapNotNull null
            }
            val argName = arg.name
            val argId = argName.nameToDiscordOption()
            val required = !arg.optional
            val value = arg.parsed?.toString()
            Triple(
                spec?.label?.translateTo(iData.language) ?: argName,
                arg.help,
                (spec?.argOption ?: ModalArgOption.Text()).buildChildComponent(
                    iData, argId, required, value
                )
            )
        }
        return Modal(createComponentId(argsBuilder, checkCompId = true), title.t()) {
            modalEntries.forEach { (name, help, child) ->
                label(
                    label = name,
                    description = help.t().ifEmpty { null },
                    child = child
                )
            }
        }
    }

    companion object {
        val eventToName: (ModalInteractionEvent) -> String = { it.modalId.substringBefore(";") }
    }
}

interface ModalKey

data class ModalArgSpec(
    val argOption: ModalArgOption = ModalArgOption.Text(),
    val modalEnableKey: ModalKey?,
    val label: K18nMessage?
) : ArgSpec

interface ModalArgOption {
    suspend fun buildChildComponent(
        iData: InteractionData,
        argId: String,
        required: Boolean,
        value: String?
    ): LabelChildComponent

    data class Text(
        private val short: Boolean = true,
        private val placeholder: K18nMessage? = null,
        private val builder: InlineTextInput.() -> Unit = {}
    ) : ModalArgOption {
        override suspend fun buildChildComponent(
            iData: InteractionData,
            argId: String,
            required: Boolean,
            value: String?
        ): LabelChildComponent {
            return TextInput(
                customId = argId,
                style = if (short) TextInputStyle.SHORT else TextInputStyle.PARAGRAPH,
                required = required,
                placeholder = placeholder?.translateTo(iData.language),
                value = value,
                builder = builder
            )
        }
    }

    data class Select(
        private val placeholder: K18nMessage? = null,
        private val valueRange: IntRange? = 1..1,
        private val optionsProvider: suspend (InteractionData) -> List<SelectOption>,
        private val builder: StringSelectMenu.Builder.() -> Unit = {}
    ) : ModalArgOption {
        override suspend fun buildChildComponent(
            iData: InteractionData,
            argId: String,
            required: Boolean,
            value: String?
        ): LabelChildComponent {
            val options = optionsProvider(iData)
            return StringSelectMenu(
                customId = argId,
                placeholder = placeholder?.translateTo(iData.language),
                valueRange = valueRange ?: 1..options.size,
                options = options,
                builder = builder,
            )
        }
    }
}