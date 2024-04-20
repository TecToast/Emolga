package de.tectoast.emolga.features.draft.during


import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.add
import de.tectoast.emolga.utils.invoke
import de.tectoast.emolga.utils.json.MDLTierlist
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.MDL
import de.tectoast.emolga.utils.json.emolga.draft.MDLPick
import de.tectoast.emolga.utils.json.emolga.draft.PickData
import de.tectoast.emolga.utils.randomWithCondition
import dev.minn.jda.ktx.messages.into
import mu.KotlinLogging
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle


object MDLCommands {
    val tiers = mapOf("S" to 1, "A" to 2, "B" to 3, "C" to 3, "D" to 2).toList()
    val germanTypeList = setOf(
        "Normal",
        "Feuer",
        "Wasser",
        "Pflanze",
        "Gestein",
        "Boden",
        "Geist",
        "Unlicht",
        "Drache",
        "Fee",
        "Eis",
        "Kampf",
        "Elektro",
        "Flug",
        "Gift",
        "Psycho",
        "Stahl",
        "Käfer"
    )
    private val logger = KotlinLogging.logger {}

    enum class MDLAction {
        ACCEPT, REROLL
    }

    object PickMDLCommand :
        CommandFeature<PickMDLCommand.Args>(::Args, CommandSpec("pickmdl", "Gamblen :)", Constants.G.VIP)) {
        class Args : Arguments() {
            var type by fromList("Typ", "Der Typ, der gewählt werden soll", germanTypeList)
        }


        context(InteractionData)
        override suspend fun exec(e: Args) {
            val d = League.byCommand()?.first ?: return reply(
                "Es läuft zurzeit kein Draft in diesem Channel!", ephemeral = true
            )
            if (d !is MDL) return reply("Dieser Command funktioniert nur im MDL Draft!")
            val mem = d.current
            val picks = d.picks[mem]!!
            val type = e.type
            var tier = ""
            var mon = ""
            val usedTiers = mutableSetOf<String>()
            for (i in 0 until 100) {
                val temptier = tiers.toMutableList().apply { removeAll { it.first in usedTiers } }
                    .randomWithCondition { it.second > picks.count { mon -> mon.tier == it.first } }?.first
                    ?: return reply("Es gibt kein $type-Pokemon mehr, welches in deinen Kader passt!")

                val tempmon = MDLTierlist.get[type]!![temptier]!!.randomWithCondition { !d.isPicked(it) }
                if (tempmon != null) {
                    tier = temptier
                    mon = tempmon
                    break
                }
                usedTiers += temptier
            }
            if (mon.isEmpty() || tier.isEmpty()) {
                logger.error("No pokemon found without error message: $mem $type")
                SendFeatures.sendToMe("ERROR PICKMDL COMMAND CONSOLE: $mem $type")
                return reply("Es ist ein unbekannter Fehler aufgetreten!")
            }
            val official = NameConventionsDB.getDiscordTranslation(mon, d.guild, false)!!.official
            d.replyGeneral(
                "gegambled: **$mon ($tier)**!",
                components = listOf(MDLButton("Akzeptieren", ButtonStyle.SUCCESS) {
                    action = MDLAction.ACCEPT
                }, MDLButton("Joker einlösen (noch ${d.jokers[mem]} übrig)", ButtonStyle.DANGER) {
                    action = MDLAction.REROLL
                }).into()
            )
            d.currentMon = MDLPick(official, mon, tier, type)
        }
    }

    object MDLButton : ButtonFeature<MDLButton.Args>(::Args, ButtonSpec("mdlpick")) {
        class Args : Arguments() {
            var action by enumBasic<MDLAction>("action", "Die Aktion, die ausgeführt werden soll")
        }


        context(InteractionData)
        override suspend fun exec(e: Args) {
            val d = League.onlyChannel(tc) ?: return reply(
                "Dieser Button funktioniert nicht mehr! Wenn du denkst, dass dies ein Fehler ist, melde dich bei ${Constants.MYTAG}.",
                ephemeral = true
            )
            if (user != d.current && user != Constants.FLOID && d.allowed[d.current]?.any { it.u == user } != true) return reply(
                "Du bist nicht dran!", ephemeral = true
            )
            if (d !is MDL) return reply("Dieser Button funktioniert nur im MDL Draft!", ephemeral = true)
            d.currentMon ?: return reply("Es gibt zurzeit keinen Pick!", ephemeral = true)
            val (official, mon, tier, type) = d.currentMon!!
            when (e.action) {
                MDLAction.ACCEPT -> {
                    replyAwait("**${d.currentMon?.tlName} (${d.currentMon?.tier})** wurde akzeptiert!")
                    val picks = d.picks(d.current)
                    d.savePick(picks, mon, tier, false)
                    val round = d.getPickRoundOfficial()
                    with(d) {
                        builder().let { b ->
                            b.pickDoc(
                                PickData(
                                    league = d,
                                    pokemon = mon,
                                    pokemonofficial = official,
                                    tier = tier,
                                    mem = d.current,
                                    round = round,
                                    freePick = false,
                                    updrafted = false
                                )
                            ).let { b }
                        }.execute()
                    }
                    d.afterPickOfficial()
                }

                MDLAction.REROLL -> {
                    val mem = d.current
                    if (d.jokers[mem]!! <= 0) return reply("Du hast keine Joker mehr!", ephemeral = true)
                    d.jokers.add(mem, -1)
                    val picks = d.picks[mem]!!
                    var newtier = ""
                    var newmon = ""
                    val usedTiers = mutableSetOf<String>()
                    for (i in 0 until 100) {
                        val temptier = tiers.toMutableList().apply { removeAll { it.first in usedTiers } }
                            .randomWithCondition { it.second > picks.count { mon -> mon.tier == it.first } }?.first
                            ?: return reply(
                                "Es gibt kein $type-Pokemon mehr, welches in deinen Kader passt!", ephemeral = true
                            )

                        val tempmon = MDLTierlist.get[type]!![temptier]!!.randomWithCondition { !d.isPicked(it) }
                        if (tempmon != null) {
                            newtier = temptier
                            newmon = tempmon
                            break
                        }
                        usedTiers += temptier
                    }
                    if (newmon.isEmpty() || newtier.isEmpty()) {
                        SendFeatures.sendToMe("ERROR PICKMDL COMMAND CONSOLE: $mem $type")
                        return reply("Es ist ein unbekannter Fehler aufgetreten!")
                    }
                    val newofficial = NameConventionsDB.getDiscordTranslation(newmon, d.guild, false)!!.official
                    replyAwait("Reroll: **${newmon} (${newtier})**!")
                    d.savePick(picks, newofficial, newtier, false)
                    val round = d.getPickRoundOfficial()
                    with(d) {
                        builder().let { b ->
                            b.pickDoc(
                                PickData(
                                    league = d,
                                    pokemon = newmon,
                                    pokemonofficial = newofficial,
                                    tier = newtier,
                                    mem = d.current,
                                    round = round,
                                    freePick = false,
                                    updrafted = false
                                )
                            ).let { b }
                        }.execute()
                    }
                    d.afterPickOfficial()
                }
            }
        }
    }

    object TeraMDLCommand :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("teramdl", "Randomized den Tera-Typen", Constants.G.VIP)) {

        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            val d =
                League.byCommand()?.first ?: return reply(
                    "Es läuft zurzeit kein Draft in diesem Channel!",
                    ephemeral = true
                )
            if (d !is MDL) {
                reply("Dieser Befehl funktioniert nur im MDL Draft!")
                return
            }
            if (!d.isLastRound) {
                reply("Dieser Befehl kann nur in der letzten Runde verwendet werden!")
                return
            }
            val type = germanTypeList.random()
            val mon = d.picks(d.current).random()
            d.replyGeneral(
                "die Terakristallisierung gegambled und den Typen `$type` auf ${mon.name} (${mon.tier}) bekommen!"
            )
            d.afterPickOfficial()
        }
    }

}



