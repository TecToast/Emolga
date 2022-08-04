package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.records.TypicalSets

class TypicalSetsCommand :
    Command("typicalsets", "Zeigt typische Moves/Items/Fähigkeiten für ein Pokemon an", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "mon", "Pokemon", "Das Pokemon",
                ArgumentManagerTemplate.draftPokemon(), false, "Das ist kein Pokemon!"
            )
            .setExample("!typicalsets Primarina")
            .build()
        slash()
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.reply(TypicalSets.buildPokemon(e.arguments.getText("mon")))
    }
}