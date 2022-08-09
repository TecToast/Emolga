package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.startsAnyIgnoreCase
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.emolga.draft.League
import org.slf4j.LoggerFactory

@Suppress("unused")
class PickCommand : Command("pick", "Pickt das Pokemon", CommandCategory.Draft) {

    init {
        //setArgumentTemplate(ArgumentManagerTemplate.noSpecifiedArgs("!pick <Pokemon> [Optionales Tier]", "!pick Emolga"));
        val prefixes = mapOf("M" to "Mega", "A" to "Alola", "G" to "Galar")
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "pokemon",
            "Pokemon",
            "Das Pokemon, was du picken willst",
            ArgumentManagerTemplate.draftPokemon { s, event ->
                val tl = Tierlist.getByGuild(event.guild!!.id)
                val strings = tl?.autoComplete?.let { acl ->
                    val ac = acl + tl.pickableNicknames
                    ac.filter {
                        it.lowercase().startsWith(s.lowercase())
                    } + ((prefixes.entries.asSequence()
                        .map { it to ac.startsAnyIgnoreCase("${it.key}-${s}") }).flatMap { pair -> pair.second.map { pair.first to it } }
                        .map { "${it.second.substringAfter("-")}-${it.first.value}" })
                }
                if (strings == null || strings.size > 25) return@draftPokemon emptyList()
                strings.sorted()
            },
            false,
            "Das ist kein Pokemon!"
        ) //.add("tier", "Tier", "Das Tier", ArgumentManagerTemplate.Text.any(), true)
            .setExample("!pick Emolga").build()
        slash(true, Constants.FPLID, Constants.NDSID, Constants.ASLID)
    }

    override suspend fun process(e: GuildCommandEvent) = exec(e, false)


    companion object {
        private val logger = LoggerFactory.getLogger(PickCommand::class.java)

        fun exec(
            e: GuildCommandEvent, isRandom: Boolean
        ) {
            val args = e.arguments
            val d = League.byChannel(e) ?: return
            val mem = d.current
            val tier: String
            val pokemon = args.getText("pokemon")
            val tierlist = d.tierlist
            val picks = d.picks[mem]!!
            if (picks.filter { it.name != "???" }.size == 15) {
                e.reply("Du hast bereits 15 Mons!")
                return
            }
            tier = if (args.has("tier") && !d.isPointBased) {
                tierlist.order.firstOrNull { args.getText("tier").equals(it, ignoreCase = true) } ?: ""
            } else {
                tierlist.getTierOf(pokemon)
            }
            if (d.isPicked(pokemon)) {
                e.reply("Dieses Pokemon wurde bereits gepickt!")
                return
            }
            val needed = tierlist.getPointsNeeded(pokemon)
            if (d.isPointBased) {
                if (needed == -1) {
                    e.reply("Das Pokemon steht nicht in der Tierliste!")
                    return
                }
            } else {
                val map = d.getPossibleTiers(mem)
                if (!map.containsKey(tier)) {
                    e.reply("Das Tier `$tier` existiert nicht!")
                    return
                }
                val origtier = tierlist.getTierOf(pokemon)
                if (origtier.isEmpty()) {
                    e.reply("Das Pokemon steht nicht in der Tierliste!")
                    return
                }
                if (tierlist.order.indexOf(origtier) < tierlist.order.indexOf(tier)) {
                    e.reply("Du kannst ein $origtier-Mon nicht ins $tier hochdraften!")
                    return
                }
                if (map[tier]!! <= 0) {
                    if (tierlist.prices[tier] == 0) {
                        e.reply("Ein Pokemon aus dem $tier-Tier musst du in ein anderes Tier hochdraften!")
                        return
                    }
                    e.reply("Du kannst dir kein $tier-Pokemon mehr picken!")
                    return
                }
            }
            if (d.handlePoints(e, needed)) return
            d.savePick(picks, pokemon, tier)
            //m.delete().queue();
            if (!isRandom) e.reply("${e.member.asMention} hat $pokemon gepickt!")
            if (isRandom) {
                e.reply("**<@$mem>** hat aus dem $tier-Tier ein **$pokemon** bekommen!")
            } else if (pokemon == "Emolga") {
                e.textChannel.sendMessage("<:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991>")
                    .queue()
            }
            d.pickDoc(PickData(pokemon, tier, mem, d.indexInRound(), picks.indexOfFirst { it.name == pokemon }))
            d.nextPlayer()
        }
    }
}

class PickData(val pokemon: String, val tier: String, val mem: Long, val indexInRound: Int, val changedIndex: Int)