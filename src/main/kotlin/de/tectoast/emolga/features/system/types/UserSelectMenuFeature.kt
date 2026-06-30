package de.tectoast.emolga.features.system.types

import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.SelectMenuSpec
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent

abstract class UserSelectMenuFeature<A : Arguments>(argsFun: () -> A, spec: SelectMenuSpec) :
    EntitySelectMenuFeature<A>(argsFun, spec) {
    override val target = EntitySelectMenu.SelectTarget.USER

    override fun getValuesFromEvent(e: EntitySelectInteractionEvent) = e.mentions.users.map { it.idLong }
}