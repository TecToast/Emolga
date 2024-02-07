package de.tectoast.emolga.features.flegmon

import com.mongodb.client.model.UpdateOptions
import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.commands.filterStartsWithIgnoreCase
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.flegmon.PepeShinyEvent.SingleGame.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.ShinyEvent
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.utils.FileUpload
import org.litote.kmongo.*
import java.net.URI

object PepeShinyEvent {
    object ShinyCommand : CommandFeature<ShinyCommand.Args>(
        ::Args, CommandSpec("shiny", "Reicht ein Shiny für das Event ein", Constants.G.PEPE)
    ) {
        class Args : Arguments() {
            var game by enumBasic<SingleGame>("spiel", "Das Spiel, in dem das Shiny gefangen wurde")
            var method by string("methode", "Die Methode, mit der das Shiny gefangen wurde") {
                slashCommand { s, event ->
                    groupedByGame[event.getOption("spiel")?.asString]?.filterStartsWithIgnoreCase(s) { it.first }
                        .convertListToAutoCompleteReply()
                }
            }
            var image by attachment("bild", "Das Bild des Shinys")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val configuration = getConfigurationByNameAndGame(e.game, e.method) ?: return reply(
                "`${e.method}` ist keine valide Methode für `${e.game}`! Nutze bitte die Autovervollständigung!",
                ephemeral = true
            )
            reply(
                "Dein Shiny wurde erfolgreich eingereicht! Sobald es approved wurde, wird es in <#$FINALCHANNEL> erscheinen.",
                ephemeral = true
            )
            jda.getTextChannelById(CHECKCHANNEL)!!
                .send("<@${user}> (${member().effectiveName}) hat ein Shiny für das Event eingereicht!\n" + "Spiel: ${e.game}\n" + "Methode: ${e.method}\n" + "(Punkte: ${configuration.points})\n" + "Bild: ${e.image.url}",
                    components = listOf(ShinyAdminButton(
                        "Bestätigen", ButtonStyle.SUCCESS, emoji = Emoji.fromUnicode("✅")
                    ) {
                        this.mode = ShinyAdminButton.Mode.APPROVE
                        this.user = self.user
                        game = e.game
                        method = e.method
                        points = configuration.points
                    }, ShinyAdminButton("Ablehnen", ButtonStyle.DANGER, Emoji.fromUnicode("❌")) {
                        this.mode = ShinyAdminButton.Mode.REJECT
                        this.user = self.user
                    }).into()
                ).queue()
        }
    }

    object ShinyAdminButton : ButtonFeature<ShinyAdminButton.Args>(::Args, ButtonSpec("shinyevent")) {
        class Args : Arguments() {
            var mode by enumBasic<Mode>()
            var user by long()
            var game by enumBasic<SingleGame>()
            var method by string()
            var points by int()
        }

        enum class Mode {
            APPROVE, REJECT
        }

        context(InteractionData)
        override suspend fun exec(e: Args) = buttonEvent {
            val uid = e.user
            when (e.mode) {
                Mode.APPROVE -> {
                    reply("Shiny wurde approved!", ephemeral = true)
                    val game = e.game
                    val method = e.method
                    val points = e.points
                    jda.getTextChannelById(FINALCHANNEL)!!.send(
                        "Neues Shiny von <@$uid>!\nSpiel: $game\nMethode: $method\nPunkte: $points"
                    ).addFiles(FileUpload.fromData(withContext(Dispatchers.IO) {
                        URI(message.contentRaw.substringAfterLast(": ")).toURL().openStream()
                    }, "shiny.png")).queue()
                    db.shinyEvent.updateOne(
                        filter = ShinyEvent::user eq uid, update = combine(
                            push(ShinyEvent::shinies, ShinyEvent.ShinyData(game.name, method)),
                            inc(ShinyEvent::points, points)
                        ), options = UpdateOptions().upsert(true)
                    )
                    message.delete().queue()
                    updateUser(uid)
                }

                Mode.REJECT -> {
                    replyModal(RejectModal {
                        user = uid
                    })
                }
            }
        }

        private suspend fun updateUser(uid: Long) {
            val filter = ShinyEvent::user eq uid
            db.shinyEvent.findOne(filter)?.let {
                val channel = EmolgaMain.flegmonjda.getTextChannelById(POINTCHANNEL)!!
                if (it.messageId == null) {
                    db.shinyEvent.updateOne(
                        filter,
                        set(ShinyEvent::messageId setTo channel.sendMessage("<@$uid>: ${it.points}").await().idLong)
                    )
                } else {
                    channel.editMessageById(it.messageId, "<@$uid>: ${it.points}").await()
                }
            }
        }
    }

    object RejectModal : ModalFeature<RejectModal.Args>(::Args, ModalSpec("")) {
        class Args : Arguments() {
            var user by long().compIdOnly()
            var reason by string("Grund") {
                default = "_Kein Grund angegeben_"
            }
        }

        override val title = "Grund eingeben"

        context(InteractionData)
        override suspend fun exec(e: Args) = modalEvent {
            val uid = e.user
            val reason = e.reason
            val m = message!!
            val url = m.contentRaw.substringAfterLast(": ")
            reply("Deine Begründung wurde an den User gesendet!", ephemeral = true)
            jda.openPrivateChannelById(uid).flatMap {
                it.sendMessage(
                    "Vielen Dank, dass du das Shiny eingereicht hast. Leider können wir das Shiny unter folgendem Grund nicht berücksichtigen: **$reason**\n\nBild-URL: $url"
                )
            }.queue()
            m.delete().queue()
        }
    }

    const val CHECKCHANNEL = 1179839622650527804
    const val FINALCHANNEL = 1030187999759192104
    const val POINTCHANNEL = 1030218100479635607

    interface Game {
        fun containsGame(game: SingleGame): Boolean
    }

    enum class SingleGame : Game {
        Gold, Silber, Kristall, Rubin, Saphir, Smaragd, Feuerrot, Blattgrün, Diamant, Perl, Platin, Heartgold, Soulsilver, Schwarz, Weiß, Schwarz2, Weiß2, X, Y, OmegaRubin, AlphaSaphir, Sonne, Mond, Ultrasonne, Ultramond, LetsGoPikachu, LetsGoEvoli, Schwert, Schild, StrahlenderDiamant, LeuchtendePerl, LegendenArceus, Karmesin, Purpur, ;

        override fun containsGame(game: SingleGame) = this == game

        operator fun plus(other: SingleGame) = CombinedGame(listOf(this, other))
    }

    class CombinedGame(private val games: List<SingleGame>) : Game {
        operator fun plus(other: SingleGame) = CombinedGame(games + other)
        operator fun plus(other: CombinedGame) = CombinedGame(games + other.games)
        override fun containsGame(game: SingleGame) = game in games
    }

    private val GSK = Gold + Silber + Kristall
    private val RSS = Rubin + Saphir + Smaragd
    private val FRBG = Feuerrot + Blattgrün
    private val DPPT = Diamant + Perl + Platin
    private val HGSS = Heartgold + Soulsilver
    private val SW = Schwarz + Weiß
    private val SW2 = Schwarz2 + Weiß2
    private val XY = X + Y
    private val ORAS = OmegaRubin + AlphaSaphir
    private val SM = Sonne + Mond
    private val USUM = Ultrasonne + Ultramond
    private val LGPE = LetsGoPikachu + LetsGoEvoli
    private val SWSH = Schwert + Schild
    private val BDSP = StrahlenderDiamant + LeuchtendePerl
    private val LA = LegendenArceus
    private val KP = Karmesin + Purpur
    private val ALL = GSK + RSS + FRBG + DPPT + HGSS + SW + SW2 + XY + ORAS + SM + USUM + LGPE + SWSH + BDSP + LA + KP


    data class Configuration(val games: Game, val points: Int)

    val config = mapOf(
        "Full Odds(Gen 2-5)" to Configuration(GSK + RSS + FRBG + DPPT + HGSS + SW + SW2, 10),
        "Full Odds(Gen 6-9)" to Configuration(XY + ORAS + SM + USUM + LGPE + SWSH + BDSP + KP, 5),
        "Full Odds Horde" to Configuration(XY + ORAS, 3),
        "Schillerpin Odds(BW2)" to Configuration(SW2, 4),
        "Schillerpin Odds(Gen6+)" to Configuration(XY + ORAS + SM + USUM + LGPE + SWSH, 3),
        "Schillerpin Odds Horde(Gen6+)" to Configuration(XY + ORAS, 1),
        "Kurios-Ei" to Configuration(Kristall, 1),
        "Ei-Methode" to Configuration(GSK, 2),
        "Masuda-Methode" to Configuration(DPPT + HGSS, 7),
        "Masuda-Methode(Gen5)" to Configuration(SW + SW2, 6),
        "Masuda-Methode Schillerpin(Gen5)" to Configuration(SW2, 5),
        "Masuda-Methode(Gen6+)" to Configuration(XY + ORAS + SM + USUM + SWSH + BDSP + KP, 2),
        "Masuda-Methode Schillerpin(Gen6+)" to Configuration(XY + ORAS + SM + USUM + SWSH + BDSP + KP, 1),
        "Pokeradar" to Configuration(DPPT + XY + BDSP, 2),
        "Kontaktsafari" to Configuration(XY, 1),
        "Chain Fishing" to Configuration(XY + ORAS, 1),
        "DexNav" to Configuration(ORAS, 3),
        "DexNav Schillerpin" to Configuration(ORAS, 2),
        "SOS-Methode" to Configuration(SM + USUM, 2),
        "Catch-Combo" to Configuration(LGPE, 2),
        "KO-Methode" to Configuration(SWSH, 3),
        "Curry" to Configuration(SWSH, 10),
        "Dynamax Abenteuer" to Configuration(SWSH, 2),
        "PLA & SV" to Configuration(KP + LA, 1),
        "NoPoints" to Configuration(ALL, 0),
    )
    val groupedByGame = SingleGame.entries.associate { game ->
        game.name to config.entries.mapNotNull { if (it.value.games.containsGame(game)) it.key.substringBefore("(") to it.value else null }
    }

    fun getConfigurationByNameAndGame(game: SingleGame, name: String): Configuration? {
        val list = groupedByGame[game.name] ?: return null
        return list.firstOrNull { it.first == name }?.second
    }
}
