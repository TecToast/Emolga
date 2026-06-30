package de.tectoast.emolga.features.system.types

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.ArgSpec
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.SelectMenuSpec
import de.tectoast.emolga.features.system.model.ArgBuilder
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

abstract class StringSelectMenuFeature<A : Arguments>(argsFun: () -> A, spec: SelectMenuSpec) :
    Feature<SelectMenuSpec, StringSelectInteractionEvent, A>(
        argsFun, spec, StringSelectInteractionEvent::class, eventToName
    ) {
    open val options: List<SelectOption>? = null
    private val selectableOptions by lazy {
        (argsFun().args.single { !it.compIdOnly }.spec as? SelectMenuArgSpec)?.selectableOptions ?: 1..1
    }
    private val isSingle by lazy { selectableOptions.let { it.first == it.last && it.first == 1 } }
    override suspend fun populateArgs(data: InteractionData, e: StringSelectInteractionEvent, args: A) {
        val (compId, regular) = args.args.partition { it.compIdOnly }
        val argsFromEvent = e.componentId.substringAfter(";").split(";")
        populateArgs(data, compId) { _, index ->
            argsFromEvent.getOrNull(index)?.takeIf { it.isNotBlank() }
        }
        val selectArg = regular.single()
        selectArg.parse(data, if (isSingle) e.values.first() else e.values)
    }

    operator fun invoke(
        placeholder: String? = null,
        options: List<SelectOption>? = this.options,
        disabled: Boolean = false,
        menuBuilder: StringSelectMenu.Builder.() -> Unit = {},
        valueRange: IntRange? = null,
        argsBuilder: ArgBuilder<A> = {},
    ) = StringSelectMenu(
        customId = createComponentId(argsBuilder, checkCompId = true),
        placeholder = placeholder,
        disabled = disabled,
        valueRange = valueRange ?: selectableOptions.let { if (it.isEmpty()) 1..(options?.size ?: 0) else it },
        options = options.orEmpty(),
        builder = menuBuilder
    )

    companion object {
        val eventToName: (StringSelectInteractionEvent) -> String = { it.componentId.substringBefore(";") }
    }
}

data class SelectMenuArgSpec(val selectableOptions: IntRange) : ArgSpec