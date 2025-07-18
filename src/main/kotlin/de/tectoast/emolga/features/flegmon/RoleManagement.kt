package de.tectoast.emolga.features.flegmon

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

        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            val g = iData.guild()
            if (iData.member().roles.any { it.idLong == ACCEPTED_RULES_ROLE }) return iData.reply(
                "Du hast die Regeln bereits akzeptiert!",
                ephemeral = true
            )
            g.addRoleToMember(iData.userObj(), g.getRoleById(ACCEPTED_RULES_ROLE)!!).queue()
            iData.reply(
                "Du hast die Regeln akzeptiert und hast jetzt Zugriff auf den Server!\nWeitere optionale Rollen kannst du dir in <#1243646697242890373> abholen.",
                ephemeral = true
            )
        }
    }

    object RoleGetMenu : SelectMenuFeature<RoleGetMenu.Args>(::Args, SelectMenuSpec("roleget")) {
        override val options = roles.map {
            SelectOption(it.name, it.compId, it.description, emoji = it.emoji)
        }

        class Args : Arguments() {
            var selection by multiOption(0..roles.size)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val (add, remove) = roles.partition { it.compId in e.selection }.toList()
                .map { it.map { r -> iData.guild().getRoleById(r.roleId)!! } }
            iData.guild().modifyMemberRoles(iData.member(), add, remove).queue()
            iData.reply("Deine Rollen wurden angepasst!", ephemeral = true)
        }
    }

    data class RoleData(
        val compId: String, val name: String, val description: String, val roleId: Long, val emoji: Emoji? = null
    )

    private val roles = listOf(
        RoleData(
            "pokemon",
            "Pokémon",
            "Alles rund um das Thema Pokémon",
            605669233715576842,
            Emoji.fromCustom("Pokeball", 967390967550332968, false)
        ),
        RoleData(
            "tcg",
            "TCG",
            "Für Sammler und Spieler der Pokémon Sammelpappe",
            796663053952352297,
            Emoji.fromCustom("TCG", 1177333576040194118, false)
        ),
        RoleData(
            "labertaschen",
            "Labertaschen",
            "Für gesellige Talks und Quatsch im Voice",
            636266356437942341,
            Emoji.fromUnicode("\uD83D\uDCAC")
        ),
        RoleData(
            "shiny",
            "Shiny Voice",
            "Für Shinyhunter, die nicht gerne alleine hunten",
            918210427798831104,
            Emoji.fromUnicode("✨")
        )
    )
}
