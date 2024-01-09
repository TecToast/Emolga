package de.tectoast.emolga.features

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.commands.filterStartsWithIgnoreCase
import de.tectoast.emolga.utils.json.emolga.draft.League

object PickFeature : CommandFeature<PickFeature.Args>(::Args, CommandSpec("pick", "Pickt ein Pokemon")) {

    class Args : Arguments() {
        var pokemon by draftPokemon("pokemon", "Das Pokemon, das gepickt werden soll")
        var tier by string("tier", "Das Tier, in dem das Pokemon gepickt werden soll") {

            slashCommand { s, event ->
                League.onlyChannel(event.channel.idLong)?.getPossibleTiers(forAutocomplete = true)
                    ?.filter { it.value > 0 }?.map { it.key }?.filterStartsWithIgnoreCase(s)
            }
        }.nullable()
        var free by boolean("free", "Ob dieser Pick ein Freepick ist") {
            default = false
        }
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {

    }
}
