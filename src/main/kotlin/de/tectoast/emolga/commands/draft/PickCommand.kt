package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.emolga.draft.League
import org.slf4j.LoggerFactory

@Suppress("unused")
class PickCommand : Command("pick", "Pickt das Pokemon", CommandCategory.Draft) {

    init {
        //setArgumentTemplate(ArgumentManagerTemplate.noSpecifiedArgs("!pick <Pokemon> [Optionales Tier]", "!pick Emolga"));
        val prefixes = mapOf(
            "M" to "Mega",
            "A" to "Alola",
            "G" to "Galar",
            "Mega" to "Mega",
            "Alola" to "Alola",
            "Galar" to "Galar"
        )
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "pokemon",
            "Pokemon",
            "Das Pokemon, was du picken willst",
            ArgumentManagerTemplate.draftPokemon({ s, event ->
                val gid = event.guild!!.idLong
                val tl = Tierlist.getByGuild(League.onlyChannel(event.channel!!.idLong)?.guild ?: gid)
                val strings = tl?.autoComplete?.let { acl ->
                    val ac = acl + tl.pickableNicknames
                    ac.filter {
                        it.lowercase().startsWith(s.lowercase())
                    } + ((prefixes.entries.asSequence()
                        .map { it to ac.filterStartsWithIgnoreCase("${it.key}-${s}") }).flatMap { pair -> pair.second.map { pair.first to it } }
                        .map { "${it.second.substringAfter("-")}-${it.first.value}" })
                }
                if (strings == null || strings.size > 25) emptyList()
                else strings.sorted()
            }, {
                Tierlist.getByGuild(League.onlyChannel(it.channel!!.idLong)?.guild ?: it.guildId)?.namepreference
                    ?: DraftNamePreference.SINGLE_CHAR_BEFORE
            }),
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
            val mem = d.current
            val tierlist = d.tierlist
            val pokemon = tierlist.getNameOf(args.getText("pokemon")) ?: run {
                e.reply("Das Pokemon steht nicht in der Tierliste!")
                return
            }
            val picks = d.picks[mem]!!
            if (picks.filter { it.name != "???" }.size == 15) {
                e.reply("Du hast bereits 15 Mons!")
                return
            }
            val (tier, origtier) = d.getTierOf(pokemon, args.getNullable("tier"))
            if (d.isPicked(pokemon)) {
                e.reply("Dieses Pokemon wurde bereits gepickt!")
                return
            }
            val needed = tierlist.getPointsNeeded(pokemon)
            val free = args.getOrDefault("free", false).takeIf { tierlist.mode.isMix() } ?: false
            if (!free && d.handleTiers(e, tier, origtier)) return
            if (d.handlePoints(e, needed, free)) return
            d.savePick(picks, pokemon, tier, free)
            //m.delete().queue();
            if (!isRandom) d.replyPick(e, pokemon, mem, free)
            if (isRandom) {
                d.replyRandomPick(e, pokemon, mem, tier)
            } else if (pokemon == "Emolga") {
                e.textChannel.sendMessage("<:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991> <:Happy:701070356386938991>")
                    .queue()
            }
            val round = d.getPickRound()
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
            d.afterPick()
        }
    }
}

class PickData(
    val pokemon: String,
    val tier: String,
    val mem: Long,
    val indexInRound: Int,
    val changedIndex: Int,
    val picks: MutableList<DraftPokemon>,
    val round: Int,
    val memIndex: Int,
    val freePick: Boolean
)