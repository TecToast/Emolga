package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.records.DexEntry
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object PokedexDB : Table("pokedex") {

    val POKEMONNAME = varchar("pokemonname", 30)
    val Rot = text("Rot").nullable()
    val Blau = text("Blau").nullable()
    val Gelb = text("Gelb").nullable()
    val Stadium = text("Stadium").nullable()
    val Gold = text("Gold").nullable()
    val Silber = text("Silber").nullable()
    val Kristall = text("Kristall").nullable()
    val Stadium2 = text("Stadium 2").nullable()
    val Rubin = text("Rubin").nullable()
    val Saphir = text("Saphir").nullable()
    val Feuerrot = text("Feuerrot").nullable()

    val Blattgruen = text("Blattgrün").nullable()
    val Smaragd = text("Smaragd").nullable()
    val Diamant = text("Diamant").nullable()
    val Perl = text("Perl").nullable()
    val Platin = text("Platin").nullable()
    val HeartGold = text("Goldene Edition HeartGold").nullable()
    val SoulSilver = text("Silberne Edition SoulSilver").nullable()
    val Schwarz = text("Schwarze Edition").nullable()
    val Weiss = text("Weiße Edition").nullable()
    val Schwarz2 = text("Schwarze Edition 2").nullable()
    val Weiss2 = text("Weiße Edition 2").nullable()
    val THREEDPro = text("3D Pro").nullable()
    val X = text("X").nullable()
    val Y = text("Y").nullable()
    val OmegaRubin = text("Omega Rubin").nullable()

    val AlphaSaphir = text("Alpha Saphir").nullable()
    val GO = text("GO").nullable()
    val Sonne = text("Sonne").nullable()
    val Mond = text("Mond").nullable()
    val Ultrasonne = text("Ultrasonne").nullable()
    val Ultramond = text("Ultramond").nullable()
    val LetsGoPikachu = text("Let's Go, Pikachu!").nullable()
    val LetsGoEvoli = text("Let's Go, Evoli!").nullable()
    val Schwert = text("Schwert").nullable()
    val Schild = text("Schild").nullable()

    val allColumns by lazy {
        listOf(
            Rot,
            Blau,
            Gelb,
            Stadium,
            Gold,
            Silber,
            Kristall,
            Stadium2,
            Rubin,
            Saphir,
            Feuerrot,
            Blattgruen,
            Smaragd,
            Diamant,
            Perl,
            Platin,
            HeartGold,
            SoulSilver,
            Schwarz,
            Weiss,
            Schwarz2,
            Weiss2,
            THREEDPro,
            X,
            Y,
            OmegaRubin,
            AlphaSaphir,
            GO,
            Sonne,
            Mond,
            Ultrasonne,
            Ultramond,
            LetsGoPikachu,
            LetsGoEvoli,
            Schwert,
            Schild
        )
    }

    fun getDexEntry(name: String) = transaction {
        val entry = select { POKEMONNAME eq NO_CHARS.replace(name, "").lowercase() }.first()
        val first = allColumns.shuffled().first { entry[it] != null }
        DexEntry(entry[first]!!, first.name)
    }

    private val NO_CHARS = Regex("[^A-Za-z]")
}
