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
            val pokemon = args.getText("pokemon")
            val picks = d.picks[mem]!!
            if (picks.count { it.name != "???" } == 15) {
                e.reply("Du hast bereits 15 Mons!")
                return
            }
            val (tier, origtier) = d.getTierOf(pokemon, args.getNullable("tier"))
            if (d.isPicked(pokemon)) {
                e.reply("Dieses Pokemon wurde bereits gepickt!")
                return
            }
            val needed = tierlist.getPointsNeeded(pokemon)
            val free = args.getOrDefault("free", false).takeIf { tierlist.mode.isTiersWithFree() } ?: false
            if (!free && d.handleTiers(e, tier, origtier)) return
            if (d.handlePoints(e, needed, free)) return
            d.savePick(picks, pokemon, tier, free)
            //m.delete().queue();
            if (!isRandom) d.replyPick(e, pokemon, free)
            if (isRandom) {
                d.replyRandomPick(e, pokemon, tier)
            } else if (pokemon == "Emolga") {
                e.textChannel.sendMessage("<:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991>")
                    .queue()
            }
            val round = d.getPickRoundOfficial()
            d.pickDoc(
                PickData(
                    pokemon,
                    tier,
                    mem,
                    d.indexInRound(round),
                    picks.indexOfFirst { it.name == pokemon },
                    picks,
                    round,
                    d.table.indexOf(mem),
                    free
                )
            )
            d.afterPickOfficial()
        }
    }
}
