package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.invoke
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.SwitchData
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

object SwitchCommand : Command("switch", "Switcht ein Pokemon", CommandCategory.Draft) {
    private val tlNameCache = SizeLimitedMap<String, String>(1000)

    init {
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "oldmon",
            "Altes Mon",
            "Das Pokemon, was rausgeschmissen werden soll",
            ArgumentManagerTemplate.draftPokemon { s, e ->
                newSuspendedTransaction {
                    League.onlyChannel(e.channel!!.idLong)?.let {
                        val tl = it.tierlist
                        it.picks[it.current]!!.filter { p -> p.name != "???" && !p.quit }.sortedWith(
                            compareBy({ mon -> tl.order.indexOf(mon.tier) },
                                { mon -> mon.name })
                        ).map { mon ->
                            logger.debug(mon.name)
                            tlNameCache[mon.name] ?: NameConventionsDB.convertOfficialToTL(
                                mon.name, it.guild
                            )!!.also { tlName -> tlNameCache[mon.name] = tlName }
                        }.filter { mon -> mon.startsWith(s, true) }
                    }
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
        slash(true, *draftGuilds)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val d =
            League.byCommand(e) ?: return e.reply("Es läuft zurzeit kein Draft in diesem Channel!", ephemeral = true)
        if (!d.isSwitchDraft) {
            return e.reply("Dieser Draft ist kein Switch-Draft, daher wird /switch nicht unterstützt!")

        }
        val mem = d.current
        d.beforeSwitch()?.let { e.reply(it); return }
        val args = e.arguments
        val oldmon = args.getDraftName("oldmon")
        val newmon = args.getDraftName("newmon")
        logger.info("Switching $oldmon to $newmon")
        val draftPokemons = d.picks(mem)
        val oldDraftMon = draftPokemons.firstOrNull { it.name == oldmon.official }
            ?: return e.reply("${oldmon.tlName} befindet sich nicht in deinem Kader!")
        val newtier = d.getTierOf(newmon.tlName, null) ?: return e.reply("Das neue Pokemon ist nicht in der Tierliste!")
        val oldtier = d.getTierOf(oldmon.tlName, null)!!.specified
        d.checkUpdraft(oldDraftMon.tier, newtier.official)?.let { e.reply(it); return }
        if (d.isPicked(newmon.official, newtier.official)) {
            e.reply("${newmon.tlName} wurde bereits gepickt!")
            return
        }
        if (d.handleTiers(e, newtier.specified, newtier.official, fromSwitch = true)) return
        if (d.handlePoints(
                e,
                tlNameNew = newmon.tlName,
                officialNew = newmon.official,
                free = false,
                tier = newtier.official,
                tierOld = oldtier
            )
        ) return
        d.replySwitch(e, oldmon.tlName, newmon.tlName)

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
                        indexInRound = indexInRound(round),
                        changedIndex = draftPokemons.indexOfFirst { it.name == newmon.official },
                        picks = draftPokemons,
                        round = round,
                        memIndex = table.indexOf(mem),
                        oldmon = oldmon.tlName,
                        oldIndex = draftPokemons.indexOfFirst { it.name == oldmon.official },
                        changedOnTeamsiteIndex = oldIndex,
                    )
                )?.let { b }
            }?.execute()
        }
        if (newmon.official == "Emolga") {
            e.textChannel.sendMessage("<:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991>")
                .queue()
        }
        d.nextPlayer()
    }


    private val logger = LoggerFactory.getLogger(SwitchCommand::class.java)

}
