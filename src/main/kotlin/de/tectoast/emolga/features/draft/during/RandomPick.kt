package de.tectoast.emolga.features.draft.during


import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.config.RandomLeaguePick
import de.tectoast.emolga.league.config.RandomPickArgument
import de.tectoast.emolga.league.config.RandomPickUserInput
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.add
import de.tectoast.emolga.utils.draft.DraftMessageType
import de.tectoast.emolga.utils.draft.DraftUtils.executeWithinLock
import de.tectoast.emolga.utils.draft.PickInput
import dev.minn.jda.ktx.messages.into
import mu.KotlinLogging
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle


object RandomPick {

    private val logger = KotlinLogging.logger {}

    enum class RandomPickAction {
        ACCEPT, REROLL
    }

    object Command :
        CommandFeature<Command.Args>(::Args, CommandSpec("randompick", "Macht einen Random-Pick", *draftGuilds)) {
        class Args : Arguments() {
            var tier by string("tier", "Das Tier, in dem gepickt werden soll") {
                slashCommand(guildChecker = {
                    val league = league() ?: return@slashCommand ArgumentPresence.OPTIONAL
                    league.config.randomPick.mode.provideCommandOptions()[RandomPickArgument.TIER]
                        ?: ArgumentPresence.NOT_PRESENT
                })
            }.nullable()
            var type by pokemontype("Typ", "Der Typ, den du haben willst", Language.ENGLISH) {
                slashCommand(guildChecker = {
                    val league = league() ?: return@slashCommand ArgumentPresence.OPTIONAL
                    league.config.randomPick.mode.provideCommandOptions()[RandomPickArgument.TYPE]
                        ?: ArgumentPresence.NOT_PRESENT
                })
            }.nullable()
        }


        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply()
            League.executePickLike {
                val config = config.randomPick
                if (!config.enabled) return iData.reply("RandomPick ist in dieser Liga deaktiviert!")
                val hasJokers = config.hasJokers()
                if (hasJokers && draftData.randomPick.currentMon?.disabled == false) return iData.reply(
                    "Du hast bereits ein Mon gegambled!",
                    ephemeral = true
                )
                val type = e.type
                val (draftname, tier) = with(config.mode) {
                    getRandomPick(
                        RandomPickUserInput(e.tier, e.type),
                        config
                    )
                } ?: return
                if (hasJokers && rerollOption(draftname, tier, type)) return
                executeWithinLock(PickInput(draftname, tier, free = false), DraftMessageType.RANDOM)
            }
        }
    }

    context(iData: InteractionData)
    private suspend fun League.rerollOption(
        draftname: DraftName,
        tier: String,
        type: String?,
        history: Set<String> = emptySet()
    ): Boolean {
        val jokerAmount = config.randomPick.jokers - (draftData.randomPick.usedJokers[current] ?: 0)
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
            draftData.randomPick.currentMon =
                RandomLeaguePick(
                    draftname.official,
                    draftname.tlName,
                    tier,
                    mapOf("type" to type),
                    history = history + draftname.official
                )
            save("RandomPick")
            return true
        }
        return false
    }

    object Button : ButtonFeature<Button.Args>(::Args, ButtonSpec("randompick")) {
        class Args : Arguments() {
            var action by enumBasic<RandomPickAction>("action", "Die Aktion, die ausgeführt werden soll")
        }


        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            iData.deferReply()
            League.executePickLike {
                val (official, tlName, tier, map, history, disabled) = draftData.randomPick.currentMon
                    ?: return iData.reply(
                    "Es gibt zurzeit keinen Pick!",
                    ephemeral = true
                )
                if (disabled) return iData.reply(
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
                        if ((draftData.randomPick.usedJokers[current]
                                ?: 0) >= config.randomPick.jokers
                        ) return iData.reply(
                            "Du hast keine Joker mehr!",
                            ephemeral = true
                        )
                        draftData.randomPick.usedJokers.add(current, 1)
                        val config = config.randomPick
                        val type = map["type"]
                        val (newdraftname, newtier) = with(config.mode) {
                            getRandomPick(RandomPickUserInput(tier, type, skipMons = history), config)
                        } ?: return
                        if (rerollOption(newdraftname, newtier, type)) return
                        executeWithinLock(PickInput(newdraftname, newtier, false), DraftMessageType.REROLL)
                    }
                }
            }
        }
    }

}



