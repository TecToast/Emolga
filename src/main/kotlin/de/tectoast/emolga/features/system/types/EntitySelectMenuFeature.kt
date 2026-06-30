package de.tectoast.emolga.features.system.types

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.SelectMenuSpec
import de.tectoast.emolga.features.system.model.ArgBuilder
import dev.minn.jda.ktx.interactions.components.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent

abstract class EntitySelectMenuFeature<A : Arguments>(argsFun: () -> A, spec: SelectMenuSpec) :
    Feature<SelectMenuSpec, EntitySelectInteractionEvent, A>(
        argsFun,
        spec,
        EntitySelectInteractionEvent::class,
        eventToName
    ) {

    abstract val target: EntitySelectMenu.SelectTarget
    private val selectableOptions by lazy {
        (argsFun().args.single { !it.compIdOnly }.spec as? SelectMenuArgSpec)?.selectableOptions ?: 1..1
    }
    private val isSingle by lazy { selectableOptions.let { it.first == it.last && it.first == 1 } }

    override suspend fun populateArgs(
        data: InteractionData,
        e: EntitySelectInteractionEvent,
        args: A
    ) {
        val (compId, regular) = args.args.partition { it.compIdOnly }
        val argsFromEvent = e.componentId.substringAfter(";").split(";")
        populateArgs(data, compId) { _, index ->
            argsFromEvent.getOrNull(index)?.takeIf { it.isNotBlank() }
        }
        val selectArg = regular.single()
        val values = getValuesFromEvent(e)
        selectArg.parse(data, if (isSingle) values.first() else values)
    }

    abstract fun getValuesFromEvent(e: EntitySelectInteractionEvent): List<Long>

    operator fun invoke(
        placeholder: String? = null,
        disabled: Boolean = false,
        menuBuilder: EntitySelectMenu.Builder.() -> Unit = {},
        valueRange: IntRange? = null,
        argsBuilder: ArgBuilder<A> = {},
    ) = EntitySelectMenu(
        customId = createComponentId(argsBuilder, checkCompId = true),
        placeholder = placeholder,
        disabled = disabled,
        valueRange = valueRange ?: selectableOptions.let { if (it.isEmpty()) 1..1 else it },
        types = listOf(target),
        builder = menuBuilder
    )

    companion object {
        val eventToName: (EntitySelectInteractionEvent) -> String = { it.componentId.substringBefore(";") }
    }
}