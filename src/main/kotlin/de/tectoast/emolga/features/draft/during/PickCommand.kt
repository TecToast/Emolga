package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.draft.DraftMessageType
import de.tectoast.emolga.utils.draft.DraftUtils
import de.tectoast.emolga.utils.draft.PickInput
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.filterStartsWithIgnoreCase
import de.tectoast.emolga.league.League

object PickCommand :
    CommandFeature<PickCommand.Args>(PickCommand::Args, CommandSpec("pick", "Pickt ein Pokemon", *draftGuilds)) {


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
            slashCommand(guildChecker = {
                Tierlist[gid]?.mode?.isTiersWithFree() == true
            })
        }
        var random by boolean("random", "RANDOMPICK (not visible)") {
            onlyInCode = true
            default = false
        }
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        League.executePickLike {
            DraftUtils.executeWithinLock(
                PickInput(e.pokemon, e.tier, e.free),
                if (e.random) DraftMessageType.RANDOM else DraftMessageType.REGULAR
            )
        }
    }

}
