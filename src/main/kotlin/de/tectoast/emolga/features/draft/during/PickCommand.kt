package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.draft.DraftMessageType
import de.tectoast.emolga.utils.draft.DraftUtils
import de.tectoast.emolga.utils.draft.PickInput
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.filterStartsWithIgnoreCase

object PickCommand :
    CommandFeature<PickCommand.Args>(PickCommand::Args, CommandSpec("pick", "Pickt ein Pokemon", *draftGuilds)) {


    class Args : Arguments() {
        var pokemon by draftPokemon("pokemon", "Das Pokemon, das gepickt werden soll")
        var tier by string("tier", "Das Tier, in dem das Pokemon gepickt werden soll") {
            slashCommand { s, event ->
                val league = League.onlyChannel(event.channel.idLong) ?: return@slashCommand null
                val current = league.currentOrFromID(event.user.idLong) ?: return@slashCommand null
                league.getPossibleTiers(forAutocomplete = true, idx = current).filter { it.value > 0 }.map { it.key }
                    .filterStartsWithIgnoreCase(s)
            }
        }.nullable()
        var free by boolean("free", "Ob dieser Pick ein Freepick ist") {
            default = false
            slashCommand(guildChecker = {
                when (Tierlist[gid]?.mode?.isTiersWithFree()) {
                    true -> ArgumentPresence.OPTIONAL
                    else -> ArgumentPresence.NOT_PRESENT
                }
            })
        }
        var tera by boolean("tera", "Ob dieser Pick dein Tera-User sein soll") {
            default = false
            slashCommand(guildChecker = {
                if (league()?.config?.teraPick != null) ArgumentPresence.OPTIONAL
                else ArgumentPresence.NOT_PRESENT
            })
        }
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        League.executePickLike {
            DraftUtils.executeWithinLock(
                PickInput(e.pokemon, e.tier, e.free, e.tera), DraftMessageType.REGULAR
            )
        }
    }

}
