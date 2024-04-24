package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.coordXMod
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.PickData
import de.tectoast.emolga.utils.json.emolga.draft.RandomPickConfig
import de.tectoast.emolga.utils.json.emolga.draft.RandomPickUserInput
import de.tectoast.emolga.utils.y

object NuzlockeCommand :
    CommandFeature<NuzlockeCommand.Args>(::Args, CommandSpec("nuzlocke", "Rerollt ein Mon", Constants.G.HELBIN)) {
    init {
        restrict(admin)
    }

    class Args : Arguments() {
        val user by member("User", "Der User, dessen Mon rerollt wird")
        val mon by draftPokemon("Mon", "Das Mon, das rerollt wird", autocomplete = { s, event ->
            val user =
                event.getOption("user")?.asMember ?: return@draftPokemon listOf("Du musst einen User zuerst angeben!")
            val league = db.leagueByGuild(event.guild?.idLong ?: -1, user.idLong)
                ?: return@draftPokemon listOf("Der User `${user.effectiveName}` nimmt an keiner Liga auf diesem Server teil!")
            monOfTeam(s, league, user.idLong)
        })
    }

    context(InteractionData) override suspend fun exec(e: Args) {
        ephemeralDefault()
        val target = e.user.idLong
        val mention = e.user.asMention
        League.executeOnFreshLock({ db.leagueByGuild(gid, target) },
            { reply("Es wurde keine Liga von $mention gefunden!") }) {
            val picks = picks[target]!!
            val index = picks.indexOfFirst { it.name == e.mon.official }
            if (index < 0) {
                return reply("Das Pokemon `${e.mon.tlName}` befindet sich nicht im Kader von $mention!")
            }
            val oldMon = picks[index]
            val config = getConfigOrDefault<RandomPickConfig>()
            val (draftname, tier) = with(config.mode) {
                getRandomPick(
                    RandomPickUserInput(tier = oldMon.tier, type = null), config
                )
            } ?: return
            picks[index] = DraftPokemon(draftname.official, tier)
            val b = builder()
            val data = PickData(
                this,
                draftname.tlName,
                draftname.official,
                tier,
                target,
                index - 1,
                freePick = false,
                updrafted = false
            )
            b.newSystemPickDoc(
                data,
                insertionIndex = index
            )
            b.addSingle(
                data.memIndex.coordXMod(
                    "Kader",
                    2,
                    5,
                    (leaguename.last().digitToInt()).y('P' - 'C', 4),
                    34,
                    25 + data.changedOnTeamsiteIndex
                ), data.pokemon
            )
            save("NuzlockeCommand")
        }
    }
}
