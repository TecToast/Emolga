package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.SwitchData
import org.slf4j.LoggerFactory

class SwitchCommand : Command("switch", "Switcht ein Pokemon", CommandCategory.Draft) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "oldmon",
            "Altes Mon",
            "Das Pokemon, was rausgeschmissen werden soll",
            ArgumentManagerTemplate.draftPokemon { s, e ->
                League.onlyChannel(e.channel!!.idLong)?.let {
                    it.picks[it.current]!!.sortedWith(
                        compareBy(
                            { mon -> it.tierlist.order.indexOf(mon.tier) },
                            { mon -> mon.name })
                    ).map { mon -> mon.name }.filter { mon -> mon.startsWith(s, true) }
                }
            },
            false,
            "Das, was du rauswerfen möchtest, ist kein Pokemon!"
        ).add(
            "newmon",
            "Neues Mon",
            "Das Pokemon, was stattdessen reinkommen soll",
            draftPokemonArgumentType,
            false,
            "Das, was du haben möchtest, ist kein Pokemon!"
        ).setExample("!switch Gufa Emolga").build()
        slash(true, *draftGuilds.toLongArray())
    }

    override suspend fun process(e: GuildCommandEvent) {
        val d = League.byChannel(e) ?: return
        if (!d.isSwitchDraft) {
            e.reply("Dieser Draft ist kein Switch-Draft, daher wird /switch nicht unterstützt!")
            return
        }
        val mem = d.current
        val args = e.arguments
        val tierlist = d.tierlist
        val oldmon = tierlist.getNameOf(args.getText("oldmon")) ?: run {
            e.reply("Das, was du rauswerfen möchtest, steht nicht in der Tierliste!")
            return
        }
        val newmon = tierlist.getNameOf(args.getText("newmon")) ?: run {
            e.reply("Das, was du haben möchtest, steht nicht in der Tierliste!")
            return
        }
        if (!d.isPickedBy(oldmon, mem)) {
            e.reply("$oldmon befindet sich nicht in deinem Kader!")
            return
        }
        if (d.isPicked(newmon)) {
            e.reply("$newmon wurde bereits gepickt!")
            return
        }
        val pointsBack = tierlist.getPointsNeeded(oldmon)
        logger.info("oldmon = $oldmon")
        logger.info("newmon = $newmon")
        val newpoints = tierlist.getPointsNeeded(newmon)
        val tier = tierlist.getTierOf(newmon)
        if (d.isPointBased) {
            if (d.points[mem]!! + pointsBack - newpoints < 0) {
                e.reply("Du kannst dir $newmon nicht leisten!")
                return
            }
            d.points[mem] = d.points[mem]!! + pointsBack - newpoints
        } else {
            if (d.getPossibleTiers()[tier]!! <= 0 && tierlist.getTierOf(oldmon) != tier) {
                e.reply("Du kannst dir kein $tier-Tier mehr holen!")
                return
            }
        }
        d.replySwitch(e, oldmon, newmon)
        val draftPokemons = d.picks[mem]!!
        d.saveSwitch(draftPokemons, oldmon, newmon, tierlist.getTierOf(newmon))
        d.switchDoc(
            SwitchData(
                newmon,
                tierlist.getTierOf(newmon),
                mem,
                d.indexInRound(d.round),
                draftPokemons.indexOfFirst { it.name == newmon },
                d.picks[mem]!!,
                d.round,
                d.table.indexOf(mem),
                oldmon,
                tierlist.getTierOf(oldmon),
            )
        )
        if (newmon == "Emolga") {
            e.textChannel.sendMessage("<:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991>")
                .queue()
        }
        d.nextPlayer()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SwitchCommand::class.java)
    }
}
