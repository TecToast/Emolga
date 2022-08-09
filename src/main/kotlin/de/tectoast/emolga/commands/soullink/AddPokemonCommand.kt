package de.tectoast.emolga.commands.soullink

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.json.Emolga

class AddPokemonCommand : Command("addpokemon", "Fügt ein Pokemon hinzu", CommandCategory.Soullink) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("location", "Location", "Die Location", ArgumentManagerTemplate.Text.any())
            .add("pokemon", "Pokemon", "Das Pokemon", ArgumentManagerTemplate.draftPokemon())
            .add(
                "status", "Status", "Der Status", ArgumentManagerTemplate.Text.of(
                    SubCommand.of("Team"),
                    SubCommand.of("Box"),
                    SubCommand.of("RIP")
                ), true
            )
            .setExample("/addpokemon Starter Robball Team")
            .build()
        slash(true, 695943416789598208L)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val soullink = Emolga.get.soullink
        val order = soullink.order
        val pokemon = args.getText("pokemon")
        val location = eachWordUpperCase(args.getText("location"))
        if (!order.contains(location)) {
            e.reply("Die Location gibt es nicht! Falls es eine neue Location ist, füge diese mit `/addlocation` hinzu.")
            return
        }
        val o = soullink.mons.getOrPut(location) { mutableMapOf() }
        o[soullinkIds[e.author.idLong]!!] = pokemon
        if (args.has("status")) o["status"] = args.getText("status")
        e.reply("\uD83D\uDC4D")
        saveEmolgaJSON()
        updateSoullink()
    }
}