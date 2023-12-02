package de.tectoast.emolga.commands.flegmon

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PepeCommand
import de.tectoast.emolga.commands.filterStartsWithIgnoreCase
import de.tectoast.emolga.commands.flegmon.ShinyCommand.SingleGame.*
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.interactions.components.danger
import dev.minn.jda.ktx.interactions.components.success
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.entities.emoji.Emoji

object ShinyCommand : PepeCommand("shiny", "Reicht ein Shiny für das Event ein") {

    interface Game {
        fun containsGame(game: SingleGame): Boolean
    }

    enum class SingleGame : Game {
        Gold, Silber, Kristall,
        Rubin, Saphir, Smaragd,
        Feuerrot, Blattgrün,
        Diamant, Perl, Platin,
        Heartgold, Soulsilver,
        Schwarz, Weiß,
        Schwarz2, Weiß2,
        X, Y,
        OmegaRubin, AlphaSaphir,
        Sonne, Mond,
        Ultrasonne, Ultramond,
        LetsGoPikachu, LetsGoEvoli,
        Schwert, Schild,
        StrahlenderDiamant, LeuchtendePerl,
        LegendenArceus,
        Karmesin, Purpur,
        ;

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

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add(
                "game",
                "Spiel",
                "Das Spiel, in dem das Shiny erhalten wurde",
                ArgumentManagerTemplate.Text.withAutocomplete { s, _ ->
                    (SingleGame.entries.filterStartsWithIgnoreCase(s).takeIf { it.size <= 25 }
                        ?: listOf("Bitte spezifiziere deine Suche!")).ifEmpty { listOf("Keine Ergebnisse!") }
                })
            add(
                "method",
                "Methode",
                "Die Hunt-Methode, mit der das Shiny erhalten wurde",
                ArgumentManagerTemplate.Text.withAutocomplete { s, event ->
                    val list =
                        groupedByGame[event.getOption("spiel")?.asString]?.filterStartsWithIgnoreCase(s) { it.first }
                    if (list?.isNotEmpty() == true) {
                        list.takeIf { it.size <= 25 } ?: listOf("Bitte spezifiziere deine Suche!")
                    } else listOf("Bitte gebe ein valides Spiel an!")
                })
            add(
                "image",
                "Bild",
                "Das Bild vom Shiny",
                ArgumentManagerTemplate.DiscordFile.of("png", "PNG", "jpg", "JPG", "jpeg", "JPEG")
            )
        }
        slash(true, Constants.G.PEPE)
    }

    fun getConfigurationByNameAndGame(game: SingleGame, name: String): Configuration? {
        val list = groupedByGame[game.name] ?: return null
        return list.firstOrNull { it.first == name }?.second
    }

    const val CHECKCHANNEL = 1179839622650527804 // placeholder
    const val FINALCHANNEL = 1030187999759192104 // placeholder
    const val POINTCHANNEL = 1030218100479635607

    override suspend fun process(e: GuildCommandEvent) {
        val gameArg = e.arguments.getText("game")
        val game = try {
            SingleGame.valueOf(gameArg)
        } catch (ex: IllegalArgumentException) {
            return e.reply_(
                "`$gameArg` ist kein valides Spiel! Nutze bitte die Autovervollständigung!",
                ephemeral = true
            )
        }
        val method = e.arguments.getText("method")
        val configuration = getConfigurationByNameAndGame(game, method) ?: return e.reply_(
            "`$method` ist keine valide Methode für `$gameArg`! Nutze bitte die Autovervollständigung!",
            ephemeral = true
        )
        e.reply_(
            "Dein Shiny wurde erfolgreich eingereicht! Sobald es approved wurde, wird es in <#1030187999759192104> erscheinen.",
            ephemeral = true
        )
        val uid = e.author.idLong
        e.jda.getTextChannelById(CHECKCHANNEL)!!.send(
            "${e.author.asMention} (${e.member.effectiveName}) hat ein Shiny für das Event eingereicht!\n" +
                    "Spiel: $gameArg\n" +
                    "Methode: $method\n" +
                    "(Punkte: ${configuration.points})\n" +
                    "Bild: ${e.arguments.getAttachment("image").url}", components = listOf(
                success(
                    "shinyevent;approve;$uid;$gameArg;$method;${configuration.points}",
                    "Bestätigen",
                    Emoji.fromUnicode("✅")
                ),
                danger("shinyevent;deny;$uid", "Ablehnen", Emoji.fromUnicode("❌"))
            ).into()
        ).queue()
    }

}
