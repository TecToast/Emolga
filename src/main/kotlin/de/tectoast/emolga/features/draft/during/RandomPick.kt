package de.tectoast.emolga.features.draft.during


import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.add
import de.tectoast.emolga.utils.draft.DraftMessageType
import de.tectoast.emolga.utils.draft.DraftUtils.executeWithinLock
import de.tectoast.emolga.utils.draft.PickInput
import de.tectoast.emolga.utils.invoke
import de.tectoast.emolga.utils.json.emolga.draft.*
import dev.minn.jda.ktx.messages.into
import mu.KotlinLogging
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle


object RandomPick {
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

    enum class RandomPickAction {
        ACCEPT, REROLL
    }

    object Command :
        CommandFeature<Command.Args>(::Args, CommandSpec("randompick", "Macht einen Random-Pick", *draftGuilds)) {
        class Args : Arguments() {
            var tier by string("tier", "Das Tier, in dem gepickt werden soll") {
                slashCommand(guildChecker = {
                    val league = league() ?: return@slashCommand false
                    league.getConfigOrDefault<RandomPickConfig>().mode.provideCommandOptions()[RandomPickArgument.TIER]
                })
            }.nullable()
            var type by fromList("Typ", "Der Typ, der gewählt werden soll", germanTypeList) {
                slashCommand(guildChecker = {
                    val league = league() ?: return@slashCommand false
                    league.getConfigOrDefault<RandomPickConfig>().mode.provideCommandOptions()[RandomPickArgument.TYPE]
                })
            }.nullable()
        }


        context(InteractionData)
        override suspend fun exec(e: Args) {
            deferReply()
            League.executePickLike {
                val config = getConfigOrDefault<RandomPickConfig>()
                if (config.disabled) return reply("RandomPick ist in dieser Liga deaktiviert!")
                val hasJokers = config.hasJokers()
                if (hasJokers && randomLeagueData.currentMon?.disabled == false) return reply(
                    "Du hast bereits ein Mon gegambled!",
                    ephemeral = true
                )
                val idx = current
                val type = e.type
                val (draftname, tier) = with(config.mode) {
                    getRandomPick(
                        RandomPickUserInput(e.tier, e.type),
                        config
                    )
                } ?: return
                if (hasJokers) {
                    val jokerAmount = randomLeagueData.jokers[idx] ?: 0
                    if (jokerAmount > 0) {
                        replyGeneral(
                            "gegambled: **${draftname.tlName}/${
                                NameConventionsDB.getSDTranslation(
                                    draftname.official, guild, english = true
                                )!!.tlName
                            } ($tier)**!",
                            components = listOf(
                                Button("Akzeptieren", ButtonStyle.SUCCESS) {
                                    action = RandomPickAction.ACCEPT
                                },
                                Button(
                                    "Joker einlösen (noch $jokerAmount übrig)",
                                    ButtonStyle.DANGER
                                ) {
                                    action = RandomPickAction.REROLL
                                }).into()
                        )
                        randomLeagueData.currentMon =
                            RandomLeaguePick(draftname.official, draftname.tlName, tier, mapOf("type" to type))
                        save("RandomPick")
                        return
                    }
                }
                executeWithinLock(PickInput(draftname, tier, free = false), DraftMessageType.RANDOM)
            }
        }
    }

    object Button : ButtonFeature<Button.Args>(::Args, ButtonSpec("randompick")) {
        class Args : Arguments() {
            var action by enumBasic<RandomPickAction>("action", "Die Aktion, die ausgeführt werden soll")
        }


        context(InteractionData)
        override suspend fun exec(e: Args) {
            deferReply()
            League.executePickLike {
                val data = randomLeagueData
                val (official, tlName, tier, map, disabled) = data.currentMon ?: return reply(
                    "Es gibt zurzeit keinen Pick!",
                    ephemeral = true
                )
                if (disabled) return reply(
                    "Du musst erstmal selbst gamblen, bevor du diesen Button verwenden kannst!",
                    ephemeral = true
                )
                when (e.action) {
                    RandomPickAction.ACCEPT -> {
                        executeWithinLock(
                            PickInput(DraftName(tlName, official), tier, free = false),
                            DraftMessageType.ACCEPT
                        )
                    }

                    RandomPickAction.REROLL -> {
                        if (data.jokers[current]!! <= 0) return reply(
                            "Du hast keine Joker mehr!",
                            ephemeral = true
                        )
                        data.jokers.add(current, -1)
                        val config = getConfigOrDefault<RandomPickConfig>()
                        val (newdraftname, newtier) = with(config.mode) {
                            getRandomPick(RandomPickUserInput(tier, map["type"], skipMon = official), config)
                        } ?: return
                        executeWithinLock(PickInput(newdraftname, newtier, false), DraftMessageType.REROLL)
                    }
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
            if (d !is RRL) {
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



