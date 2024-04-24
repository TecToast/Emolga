package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.invoke
import de.tectoast.emolga.utils.json.emolga.draft.BypassCurrentPlayerData
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.SwitchData
import mu.KotlinLogging

object SwitchCommand :
    CommandFeature<SwitchCommand.Args>(::Args, CommandSpec("switch", "Switcht ein Pokemon", *draftGuilds)) {

    private val logger = KotlinLogging.logger {}

    class Args : Arguments() {
        var oldmon by draftPokemon(
            "Altes Mon",
            "Das Pokemon, was rausgeschmissen werden soll",
            autocomplete = { s, event ->
                val league = League.onlyChannel(event.channelIdLong) ?: return@draftPokemon null
                monOfTeam(s, league, league.current)
            }
        )
        var newmon by draftPokemon("Neues Mon", "Das Pokemon, was stattdessen reinkommen soll")
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        val d =
            League.byCommand()?.first ?: return reply(
                "Es läuft zurzeit kein Draft in diesem Channel!",
                ephemeral = true
            )
        d.lockForPick(BypassCurrentPlayerData.No) l@{
            if (!d.isSwitchDraft) {
                return@l reply("Dieser Draft ist kein Switch-Draft, daher wird /switch nicht unterstützt!")
            }
            val mem = d.current
            d.beforeSwitch()?.let { reply(it); return@l }
            val oldmon = e.oldmon
            val newmon = e.newmon
            logger.info("Switching $oldmon to $newmon")
            val draftPokemons = d.picks(mem)
            val oldDraftMon = draftPokemons.firstOrNull { it.name == oldmon.official }
                ?: return@l reply("${oldmon.tlName} befindet sich nicht in deinem Kader!")
            val newtier =
                d.getTierOf(newmon.tlName, null) ?: return@l reply("Das neue Pokemon ist nicht in der Tierliste!")
            val oldtier = d.getTierOf(oldmon.tlName, null)!!.specified
            d.checkUpdraft(oldDraftMon.tier, newtier.official)?.let { reply(it); return@l }
            if (d.isPicked(newmon.official, newtier.official)) {
                return@l reply("${newmon.tlName} wurde bereits gepickt!")
            }
            if (d.handleTiers(newtier.specified, newtier.official, fromSwitch = true)) return@l
            if (d.handlePoints(
                    free = false,
                    tier = newtier.official,
                    tierOld = oldtier
                )
            ) return@l
            d.replySwitch(oldmon.tlName, newmon.tlName)

            val oldIndex = d.saveSwitch(draftPokemons, oldmon.official, newmon.official, newtier.specified)
            with(d) {
                builder().let { b ->
                    b.switchDoc(
                        SwitchData(
                            league = this,
                            pokemon = newmon.tlName,
                            pokemonofficial = newmon.official,
                            tier = newtier.specified,
                            mem = mem,
                            round = round,
                            oldmon = oldmon.tlName,
                            oldIndex = draftPokemons.indexOfFirst { it.name == oldmon.official },
                            changedOnTeamsiteIndex = oldIndex,
                        )
                    )?.let { b }
                }?.execute()
            }
            if (newmon.official == "Emolga") {
                sendMessage("<:Happy:967390966153609226> ".repeat(5))
            }
            d.afterPickOfficial()
        }
    }
}
