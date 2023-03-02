package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.Tierlist

class TierCommand :
    Command("tier", "Zeigt das Tier des Pokemon in der Liga dieses Servers an.", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("mon", "Pokemon", "Das Pokemon", ArgumentManagerTemplate.draftPokemon())
            .setExample("!tier M-Galagladi")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        fun replyError() =
            e.reply("Auf diesem Server ist keine Tierliste hinterlegt! Wenn du dies tun möchtest, melde dich bei ${Constants.MYTAG}.")

        val tierlist = Tierlist[e.guild.idLong]
            ?: return replyError()
        val pkmn = getDraftGerName(e.arguments.getText("mon"), e.guild.idLong)?.tlName ?: return replyError()
        val tier = tierlist.getTierOf(pkmn)
        e.reply(if (tier.isNotEmpty()) "$pkmn ist im $tier-Tier!" else "$pkmn befindet sich nicht in der Tierliste!")
    }
}
