package de.tectoast.emolga.features.flegmon

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.features.*
import dev.minn.jda.ktx.interactions.components.SelectOption
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

object RoleManagement {
    private const val ACCEPTED_RULES_ROLE = 605635673885507614

    object RuleAcceptButton : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("ruleaccept")) {
        override val label = "Regeln akzeptieren"
        override val buttonStyle = ButtonStyle.PRIMARY
        override val emoji = Emoji.fromUnicode("✅")

        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            val g = guild()
            g.addRoleToMember(user(), g.getRoleById(ACCEPTED_RULES_ROLE)!!).queue()
            reply("Du hast die Regeln akzeptiert und hast jetzt Zugriff auf den Server!", ephemeral = true)
        }
    }

    object RoleGetMenu : SelectMenuFeature<RoleGetMenu.Args>(::Args, SelectMenuSpec("roleget")) {
        override val options = roles.map {
            SelectOption(it.name, it.compId, it.description, emoji = it.emoji)
        }

        class Args : Arguments() {
            var selection by multiOption(roles.indices)
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val (add, remove) = roles.partition { it.compId in e.selection }.toList()
                .map { it.map { r -> guild().getRoleById(r.roleId)!! } }
            guild().modifyMemberRoles(member(), add, remove).queue()
            reply("Deine Rollen wurden angepasst!", ephemeral = true)
        }
    }

    data class RoleData(
        val compId: String, val name: String, val description: String, val roleId: Long, val emoji: Emoji? = null
    )

    private val roles = listOf(
        RoleData(
            "pokemon",
            "Pokémon",
            "Pokémon whatever",
            605669233715576842,
            Emoji.fromCustom("Pokeball", 967390967550332968, false)
        ),
        RoleData(
            "tcg",
            "TCG",
            "TCG whatever",
            1177356119576940687,
            Emoji.fromCustom("TCG", 796663053952352297, false)
        ),
        RoleData("labertaschen", "Labertaschen", "Labertaschen whatever", 636266356437942341),
        RoleData("shiny", "Shiny Voice", "Shiny Voice whatever", 918210427798831104, Emoji.fromUnicode("✨"))
    )
}
