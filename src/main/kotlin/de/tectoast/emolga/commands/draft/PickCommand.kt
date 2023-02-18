package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.PickData
import org.slf4j.LoggerFactory

@Suppress("unused")
class PickCommand : Command("pick", "Pickt das Pokemon", CommandCategory.Draft) {

    init {
        //setArgumentTemplate(ArgumentManagerTemplate.noSpecifiedArgs("!pick <Pokemon> [Optionales Tier]", "!pick Emolga"));
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "pokemon",
            "Pokemon",
            "Das Pokemon, was du picken willst",
            draftPokemonArgumentType,
            false,
            "Das ist kein Pokemon!"
        ).add("tier", "Tier", "Das Tier", ArgumentManagerTemplate.Text.draftTiers(), true)
            .add("free", "Free-Pick", "Ob dieser Pick ein Freepick ist", ArgumentManagerTemplate.ArgumentBoolean, true)
            .setExample("!pick Emolga").build()
        slash(true, *draftGuilds.toLongArray())
    }

    override suspend fun process(e: GuildCommandEvent) = exec(e, false)


    companion object {
        private val logger = LoggerFactory.getLogger(PickCommand::class.java)

        suspend fun exec(
            e: GuildCommandEvent, isRandom: Boolean
        ) {
            val args = e.arguments
            val d = League.byChannel(e) ?: return
            if (d.isSwitchDraft && !d.allowPickDuringSwitch) {
                e.reply("Du kannst während des Switch-Drafts nicht picken!")
                return
            }
            val mem = d.current
            val tierlist = d.tierlist
            d.beforePick()?.let { e.reply(it); return }
            val (tlName, official, _) = args.getDraftName("pokemon")
            println("tlName: $tlName, official: $official")
            val picks = d.picks[mem]!!
            if (picks.count { it.name != "???" } == 15) {
                e.reply("Du hast bereits 15 Mons!")
                return
            }
            val (tier, origtier) = d.getTierOf(tlName, args.getNullable("tier"))
            if (d.isPicked(official)) {
                e.reply("Dieses Pokemon wurde bereits gepickt!")
                return
            }
            val tlMode = tierlist.mode
            val free = args.getOrDefault("free", false).takeIf { tlMode.isTiersWithFree() } ?: false
            if (!free && tlMode.withTiers && d.handleTiers(e, tier, origtier)) return
            if (tlMode.withPoints && d.handlePoints(e, tierlist.getPointsNeeded(tlName), free)) return
            d.savePick(picks, official, tier, free)
            //m.delete().queue();
            if (!isRandom) d.replyPick(e, tlName, free)
            if (isRandom) {
                d.replyRandomPick(e, tlName, tier)
            } else if (official == "Emolga") {
                e.textChannel.sendMessage("<:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991>")
                    .queue()
            }
            val round = d.getPickRoundOfficial()
            d.pickDoc(
                PickData(
                    pokemon = tlName,
                    tier = tier,
                    mem = mem,
                    indexInRound = d.indexInRound(round),
                    changedIndex = picks.indexOfFirst { it.name == official },
                    picks = picks,
                    round = round,
                    memIndex = d.table.indexOf(mem),
                    freePick = free
                )
            )
            d.afterPickOfficial()
        }
    }
}
