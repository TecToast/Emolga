package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import dev.minn.jda.ktx.interactions.components.primary
import net.dv8tion.jda.api.entities.emoji.Emoji

class RandomTeamCommand : Command("randomteam", "Generiert ein Random Team", CommandCategory.Pokemon, Constants.G.ASL) {

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.reply(
            buildString(e.author.idLong),
            ma = {
                it.setActionRow(
                    primary(
                        "randomteam;gamble",
                        "Gamble again :)",
                        emoji = Emoji.fromUnicode("\uD83D\uDD01")
                    )
                )
            })
    }

    companion object {
        private val sets = listOf(
            mapOf("S" to 4, "A" to 0, "B" to 2, "C" to 4, "D" to 2),
            mapOf("S" to 3, "A" to 2, "B" to 1, "C" to 4, "D" to 2),
            mapOf("S" to 3, "A" to 1, "B" to 3, "C" to 3, "D" to 2),
            mapOf("S" to 2, "A" to 3, "B" to 2, "C" to 3, "D" to 2),
            mapOf("S" to 2, "A" to 2, "B" to 4, "C" to 2, "D" to 2),
            mapOf("S" to 2, "A" to 3, "B" to 2, "C" to 3, "D" to 2),
        )

        fun buildString(id: Long): String =
            "Team für <@$id>:\n" + generateTeam().joinToString("\n") { "${it.tier}: ${it.name}" }

        fun generateTeam(): List<DraftPokemon> {
            return Tierlist[Constants.G.VIP]!!.retrieveTierlistMap(sets.random())
        }
    }
}
