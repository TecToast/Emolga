package de.tectoast.emolga.selectmenus

import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow

object RoleGetMenu : MenuListener("roleget") {
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

    override suspend fun process(e: StringSelectInteractionEvent, menuname: String?) {
        val (add, remove) = roles.partition { it.compId in e.values }.toList()
            .map { it.map { r -> e.jda.getRoleById(r.roleId)!! } }
        e.guild!!.modifyMemberRoles(e.member!!, add, remove).queue()
        e.reply("Deine Rollen wurden angepasst!").setEphemeral(true).queue()
    }

    fun getActionRows() = ActionRow.of(StringSelectMenu("roleget", valueRange = roles.indices, options = roles.map {
        SelectOption(it.name, it.compId, it.description, emoji = it.emoji)
    }))


}
