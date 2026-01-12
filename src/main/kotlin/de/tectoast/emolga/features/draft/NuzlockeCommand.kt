package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.PickData
import de.tectoast.emolga.league.config.RandomPickUserInput
import de.tectoast.emolga.utils.coordXMod
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.y
import dev.minn.jda.ktx.coroutines.await

object NuzlockeCommand :
    CommandFeature<NuzlockeCommand.Args>(::Args, CommandSpec("nuzlocke", "Rerollt ein Mon")) {

    init {
        restrict(admin)
    }

    class Args : Arguments() {
        val user by member("User", "Der User, dessen Mon rerollt wird")
        val mon by draftPokemon("Mon", "Das Mon, das rerollt wird", autocomplete = { s, event ->
            val user =
                event.getOption("user")?.asString?.toLongOrNull()
                    ?: return@draftPokemon listOf("Du musst einen User zuerst angeben!")
            val league = db.leagueByGuild(event.guild?.idLong ?: -1, user)
                ?: return@draftPokemon listOf("Der angegebene User nimmt an keiner Liga auf diesem Server teil!")
            monOfTeam(s, league, league(user))
        })
        val newMon by draftPokemon(
            "NewMonIfWished",
            "Falls das Pokemon manuell entschieden wurde, hier angeben"
        ).nullable()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply()
        executeMonSwitch(e.user.idLong, e.mon, e.newMon)
    }

    context(iData: InteractionData)
    suspend fun executeMonSwitch(target: Long, mon: DraftName, newMon: DraftName? = null) {
        val mention = "<@$target>"
        League.executeOnFreshLock(
            { db.leagueByGuild(iData.gid, target) },
            {
                iData.reply(
                    "Es wurde keine Liga von `${
                        iData.guild().retrieveMemberById(target).await().effectiveName
                    }` gefunden!"
                )
            }) l@{
            val idx = this(target)
            val picks = picks[idx]!!
            val index = picks.indexOfFirst { it.name == mon.official }
                .let { if (it == -1) picks.indexOfFirst { p -> p.name in mon.official } else it }
            if (index < 0) {
                return@l iData.reply("Das Pokemon `${mon.tlName}` befindet sich nicht im Kader von $mention!")
            }
            val oldMon = picks[index]
            val (draftname, tier) = newMon?.let {
                it to tierlist.getTierOf(it.official)!!
            } ?: run {
                val config = config.randomPick
                with(config.mode) {
                    getRandomPick(
                        RandomPickUserInput(
                            tier = oldMon.tier,
                            type = null,
                            ignoreRestrictions = true,
                            skipMons = setOf(mon.official)
                        ), config
                    )
                } ?: return@l
            }
            picks[index] = DraftPokemon(draftname.official, tier)
            val b = builder()
            val data = PickData(
                this,
                draftname.tlName,
                draftname.official,
                tier,
                idx,
                index - 1,
                freePick = false,
                updrafted = false,
                tera = false
            )
            b.newSystemPickDoc(
                data,
                insertionIndex = index
            )
            b.addSingle(
                data.idx.coordXMod(
                    "Kader",
                    2,
                    5,
                    (leaguename.last().digitToInt()).y('P' - 'C', 4),
                    34,
                    25 + data.getTierInsertIndex(index + 1)
                ), data.pokemon
            )
            b.execute(true)
            iData.reply(
                "Das Pokemon `${mon.tlName}` von $mention wurde zu `${draftname.tlName}/${
                    NameConventionsDB.getSDTranslation(
                        draftname.official,
                        iData.gid,
                        english = true
                    )?.tlName
                }` rerollt!"
            )
            save()
        }
    }
}
