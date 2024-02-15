package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.filterStartsWithIgnoreCase
import de.tectoast.emolga.utils.invoke
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.PickData
import de.tectoast.emolga.utils.json.emolga.draft.isMega

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
        }
        var random by boolean("random", "RANDOMPICK (not visible)") {
            onlyInCode = true
            default = false
        }
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        val dd = League.byCommand() ?: return run {
            if (!acknowledged) {
                reply(
                    "Es läuft zurzeit kein Draft in diesem Channel!",
                    ephemeral = true
                )
            }
        }
        val (d, data) = dd
        d.lockForPick(data) l@{
            if (d.isSwitchDraft && !d.allowPickDuringSwitch) {
                return@l reply("Du kannst während des Switch-Drafts nicht picken!")

            }
            d.beforePick()?.let { return@l reply(it) }
            val mem = d.current
            val tierlist = d.tierlist
            val picks = d.picks(mem)
            val (tlName, official, _) = e.pokemon
            println("tlName: $tlName, official: $official")
            val (specifiedTier, officialTier) =
                (d.getTierOf(tlName, e.tier)
                    ?: return@l reply("Dieses Pokemon ist nicht in der Tierliste!"))

            d.checkUpdraft(specifiedTier, officialTier)?.let { return@l reply(it) }
            if (d.isPicked(official, officialTier)) return@l reply("Dieses Pokemon wurde bereits gepickt!")
            val tlMode = tierlist.mode
            val free = e.free
                .takeIf { tlMode.isTiersWithFree() && !(tierlist.variableMegaPrice && official.isMega) } ?: false
            if (!free && d.handleTiers(specifiedTier, officialTier)) return@l
            if (d.handlePoints(
                    tlNameNew = tlName,
                    officialNew = official,
                    free = free,
                    tier = officialTier
                )
            ) return@l
            val saveTier = if (free) officialTier else specifiedTier
            d.savePick(picks, official, saveTier, free)
            //m.delete().queue();
            if (!e.random) d.replyPick(tlName, free, specifiedTier.takeIf { saveTier != officialTier })
            if (e.random) {
                d.replyRandomPick(tlName, specifiedTier)
            } else if (official == "Emolga") {
                sendMessage("<:Happy:967390966153609226> ".repeat(5))
            }
            val round = d.getPickRoundOfficial()
            with(d) {
                builder().let { b ->
                    b.pickDoc(
                        PickData(
                            league = d,
                            pokemon = tlName,
                            pokemonofficial = official,
                            tier = saveTier,
                            mem = mem,
                            indexInRound = indexInRound(round),
                            changedIndex = picks.indexOfFirst { it.name == official },
                            picks = picks,
                            round = round,
                            memIndex = table.indexOf(mem),
                            freePick = free
                        )
                    )?.let { b }
                }?.execute()
            }
            d.afterPickOfficial()
        }
    }
}
