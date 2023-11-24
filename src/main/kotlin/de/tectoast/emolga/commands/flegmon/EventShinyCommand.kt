package de.tectoast.emolga.commands.flegmon

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PepeCommand
import de.tectoast.emolga.commands.flegmon.EventShinyCommand.SingleGame.*

object EventShinyCommand : PepeCommand("eventshiny", "Reicht ein Shiny für das Event ein") {

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

    class CombinedGame(val games: List<SingleGame>) : Game {
        operator fun plus(other: SingleGame) = CombinedGame(games + other)
        operator fun plus(other: CombinedGame) = CombinedGame(games + other.games)
        override fun containsGame(game: SingleGame) = game in games
    }

    val GSK = Gold + Silber + Kristall
    val RSS = Rubin + Saphir + Smaragd
    val FRBG = Feuerrot + Blattgrün
    val DPPT = Diamant + Perl + Platin
    val HGSS = Heartgold + Soulsilver
    val SW = Schwarz + Weiß
    val SW2 = Schwarz2 + Weiß2
    val XY = X + Y
    val ORAS = OmegaRubin + AlphaSaphir
    val SM = Sonne + Mond
    val USUM = Ultrasonne + Ultramond
    val LGPE = LetsGoPikachu + LetsGoEvoli
    val SWSH = Schwert + Schild
    val BDSP = StrahlenderDiamant + LeuchtendePerl
    val LA = LegendenArceus
    val KP = Karmesin + Purpur


    data class Configuration(val games: Game, val points: Int)

    val config = mapOf(
        "Full Odds(Gen 2-5)" to Configuration(GSK + RSS + FRBG + DPPT + HGSS + SW + SW2, 10),
        "Full Odds(Gen6+)" to Configuration(XY + ORAS + SM + USUM + LGPE + SWSH + BDSP, 5),
        "Full Odds Horde(Gen6+)" to Configuration(XY + ORAS + SM + USUM + LGPE + SWSH + BDSP, 4),
        "Kurios-Ei" to Configuration(Kristall, 1),
        "Ei-Methode" to Configuration(GSK, 2),
        "Pokeradar" to Configuration(DPPT, 3),
        "Schillerpin Shiny" to Configuration(SW2, 4),
        "Masuda-Methode" to Configuration(DPPT + HGSS, 7),
        "Masuda-Methode ohne Pin" to Configuration(SW + SW2, 6),
        "Masuda-Methode mit Pin" to Configuration(SW2, 5),
        "Schillerpin Shiny(Gen6+)" to Configuration(XY + ORAS + SM + USUM + LGPE + SWSH, 3),
        "Schillerpin Shiny Horde(Gen6+)" to Configuration(XY + ORAS + SM + USUM + LGPE + SWSH, 2),
        "Kontaktsafari" to Configuration(XY, 1),
        "Pokeradar" to Configuration(XY + BDSP, 2),
        "DexNav ohne Pin" to Configuration(ORAS, 3),
        "DexNav mit Pin" to Configuration(ORAS, 2),
        "Masuda-Methode ohne Pin(Gen6+)" to Configuration(XY + ORAS + SM + USUM + SWSH + BDSP + KP, 2),
        "Masuda-Methode mit Pin(Gen6+)" to Configuration(XY + ORAS + SM + USUM + SWSH + BDSP + KP, 1),
        "Chain Fishing" to Configuration(XY + ORAS, 1),
        "SOS-Methode" to Configuration(SM + USUM, 2),
        "Catch-Combo" to Configuration(LGPE, 2),
        "Murder-Method" to Configuration(SWSH, 3),
        "Curry" to Configuration(SWSH, 10),
        "Dynamax Abenteuer" to Configuration(SWSH, 2),
        "PLA & SV" to Configuration(KP + LA, 1)
    )

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            TODO()
        }
    }

    override suspend fun process(e: GuildCommandEvent) {
        TODO()
    }

}
