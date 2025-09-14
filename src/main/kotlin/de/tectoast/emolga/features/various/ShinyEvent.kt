@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.features.various

import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.various.ShinyEvent.SingleGame.*
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.filterStartsWithIgnoreCase
import de.tectoast.emolga.utils.json.ShinyEventResult
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.utils.FileUpload
import org.litote.kmongo.*
import java.net.URI
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object ShinyEvent {

    val guildToEvent = OneTimeCache {
        db.shinyEventConfig.find().toList().associateBy { it.guild }
    }

    object ShinyCommand : CommandFeature<ShinyCommand.Args>(
        ::Args, CommandSpec("shiny", "Reicht ein Shiny für das Event ein")
    ) {
        class Args : Arguments() {
            var game by enumBasic<SingleGame>("spiel", "Das Spiel, in dem das Shiny gefangen wurde")
            var method by string("methode", "Die Methode, mit der das Shiny gefangen wurde") {
                slashCommand { s, event ->
                    val config =
                        guildToEvent()[event.guild?.idLong] ?: return@slashCommand listOf("Derzeit kein Event aktiv!")
                    config.groupedByGame[event.getOption("spiel")?.asString]?.filterStartsWithIgnoreCase(s) { it.first }
                        .convertListToAutoCompleteReply()
                }
            }
            var image by attachment("bild", "Das Bild des Shinys")
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val config = guildToEvent()[iData.gid] ?: return iData.reply("Derzeit kein Event aktiv!", ephemeral = true)
            val configuration = config.getConfigurationByNameAndGame(e.game, e.method) ?: return iData.reply(
                "`${e.method}` ist keine valide Methode für `${e.game}`! Nutze bitte die Autovervollständigung!",
                ephemeral = true
            )
            iData.reply(
                "Dein Shiny wurde erfolgreich eingereicht! Sobald es approved wurde, wird es in <#${config.finalChannel}> erscheinen.",
                ephemeral = true
            )
            iData.jda.getTextChannelById(config.checkChannel)!!
                .send(
                    "<@${iData.user}> (${iData.member().effectiveName}) hat ein Shiny für das Event eingereicht!\n" + "Spiel: ${e.game}\n" + "Methode: ${e.method}\n" + "(Punkte: ${configuration.points})\n" + "Bild: ${e.image.url}",
                    components = listOf(ShinyAdminButton(
                        "Bestätigen", ButtonStyle.SUCCESS, emoji = Emoji.fromUnicode("✅")
                    ) {
                        this.eventName = config.name
                        this.mode = ShinyAdminButton.Mode.APPROVE
                        this.user = iData.user
                        game = e.game
                        method = e.method
                        points = configuration.points
                    }, ShinyAdminButton("Ablehnen", ButtonStyle.DANGER, Emoji.fromUnicode("❌")) {
                        this.mode = ShinyAdminButton.Mode.REJECT
                        this.user = iData.user
                    }).into()
                ).queue()
        }
    }

    object ShinyAdminButton : ButtonFeature<ShinyAdminButton.Args>(::Args, ButtonSpec("shinyevent")) {
        class Args : Arguments() {
            var eventName by string()
            var mode by enumBasic<Mode>()
            var user by long()
            var game by enumBasic<SingleGame>()
            var method by string()
            var points by int()
        }

        enum class Mode {
            APPROVE, REJECT
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val uid = e.user
            val config = guildToEvent()[iData.gid] ?: return iData.reply("Derzeit kein Event aktiv!", ephemeral = true)
            when (e.mode) {
                Mode.APPROVE -> {
                    iData.reply("Shiny wurde approved!", ephemeral = true)
                    val game = e.game
                    val method = e.method
                    val points = e.points
                    iData.jda.getTextChannelById(config.finalChannel)!!.send(
                        "Neues Shiny von <@$uid>!\nSpiel: $game\nMethode: $method\nPunkte: $points"
                    ).addFiles(FileUpload.fromData(withContext(Dispatchers.IO) {
                        URI(iData.message.contentRaw.substringAfterLast(": ")).toURL().openStream()
                    }, "shiny.png")).queue()
                    db.shinyEventResults.updateOne(
                        filter = and(ShinyEventResult::eventName eq e.eventName, ShinyEventResult::user eq uid),
                        update = combine(
                            push(
                                ShinyEventResult::shinies,
                                ShinyEventResult.ShinyData(game.name, method, Clock.System.now())
                            ),
                            inc(ShinyEventResult::points, points)
                        ),
                        options = upsert()
                    )
                    iData.message.delete().queue()
                    config.updateDiscord(iData.jda)
                }

                Mode.REJECT -> {
                    iData.replyModal(RejectModal {
                        user = uid
                    })
                }
            }
        }


    }

    object RejectModal : ModalFeature<RejectModal.Args>(::Args, ModalSpec("shinyeventreject")) {
        class Args : Arguments() {
            var user by long().compIdOnly()
            var reason by string("Grund") {
                default = "_Kein Grund angegeben_"
            }
        }

        override val title = "Grund eingeben"

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val uid = e.user
            val reason = e.reason
            val url = iData.message.contentRaw.substringAfterLast(": ")
            iData.reply("Deine Begründung wurde an den User gesendet!", ephemeral = true)
            iData.jda.openPrivateChannelById(uid).flatMap {
                it.sendMessage(
                    "Vielen Dank, dass du das Shiny eingereicht hast. Leider können wir das Shiny unter folgendem Grund nicht berücksichtigen: **$reason**\n\nBild-URL: $url"
                )
            }.queue()
            iData.message.delete().queue()
        }
    }

    interface Game {
        val games: Set<SingleGame>
        operator fun plus(other: Game): Game = CombinedGame(games + other.games)
    }

    enum class SingleGame : Game {
        Gold, Silber, Kristall, Rubin, Saphir, Smaragd, Feuerrot, Blattgrün, Diamant, Perl, Platin, Heartgold, Soulsilver, Schwarz, Weiß, Schwarz2, Weiß2, X, Y, OmegaRubin, AlphaSaphir, Sonne, Mond, Ultrasonne, Ultramond, LetsGoPikachu, LetsGoEvoli, Schwert, Schild, StrahlenderDiamant, LeuchtendePerl, LegendenArceus, Karmesin, Purpur, ;

        override val games: Set<SingleGame> = setOf(this)
    }

    class CombinedGame(override val games: Set<SingleGame>) : Game


    val groupedGames = mutableMapOf(
        "GSK" to Gold + Silber + Kristall,
        "RSS" to Rubin + Saphir + Smaragd,
        "FRBG" to Feuerrot + Blattgrün,
        "DPPT" to Diamant + Perl + Platin,
        "HGSS" to Heartgold + Soulsilver,
        "SW" to Schwarz + Weiß,
        "SW2" to Schwarz2 + Weiß2,
        "XY" to X + Y,
        "ORAS" to OmegaRubin + AlphaSaphir,
        "SM" to Sonne + Mond,
        "USUM" to Ultrasonne + Ultramond,
        "LGPE" to LetsGoPikachu + LetsGoEvoli,
        "SWSH" to Schwert + Schild,
        "SDLP" to StrahlenderDiamant + LeuchtendePerl,
        "PLA" to LegendenArceus,
        "KP" to Karmesin + Purpur,
    ).apply {
        put("ALL", values.reduce { acc, game -> acc + game })
    }


//    val config = mapOf(
//        "Full Odds(Gen 2-5)" to Configuration(GSK + RSS + FRBG + DPPT + HGSS + SW + SW2, 10),
//        "Full Odds(Gen 6-9)" to Configuration(XY + ORAS + SM + USUM + LGPE + SWSH + SDLP + KP, 5),
//        "Full Odds Horde" to Configuration(XY + ORAS, 3),
//        "Schillerpin Odds(BW2)" to Configuration(SW2, 4),
//        "Schillerpin Odds(Gen6+)" to Configuration(XY + ORAS + SM + USUM + LGPE + SWSH, 3),
//        "Schillerpin Odds Horde(Gen6+)" to Configuration(XY + ORAS, 1),
//        "Kurios-Ei" to Configuration(Kristall, 1),
//        "Ei-Methode" to Configuration(GSK, 2),
//        "Masuda-Methode" to Configuration(DPPT + HGSS, 7),
//        "Masuda-Methode(Gen5)" to Configuration(SW + SW2, 6),
//        "Masuda-Methode Schillerpin(Gen5)" to Configuration(SW2, 5),
//        "Masuda-Methode(Gen6+)" to Configuration(XY + ORAS + SM + USUM + SWSH + SDLP + KP, 2),
//        "Masuda-Methode Schillerpin(Gen6+)" to Configuration(XY + ORAS + SM + USUM + SWSH + SDLP + KP, 1),
//        "Pokeradar" to Configuration(DPPT + XY + SDLP, 2),
//        "Kontaktsafari" to Configuration(XY, 1),
//        "Chain Fishing" to Configuration(XY + ORAS, 1),
//        "DexNav" to Configuration(ORAS, 3),
//        "DexNav Schillerpin" to Configuration(ORAS, 2),
//        "SOS-Methode" to Configuration(SM + USUM, 2),
//        "Catch-Combo" to Configuration(LGPE, 2),
//        "KO-Methode" to Configuration(SWSH, 3),
//        "Curry" to Configuration(SWSH, 10),
//        "Dynamax Abenteuer" to Configuration(SWSH, 2),
//        "PLA & SV" to Configuration(KP + PLA, 1),
//        "NoPoints" to Configuration(ALL, 0),
//    )


}
