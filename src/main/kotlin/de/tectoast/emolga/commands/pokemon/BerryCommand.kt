package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class BerryCommand : Command("berry", "Zeigt den Namen der Antibeere für diesen Typen an.", CommandCategory.Pokemon) {
    init {
        argumentTemplate =
            ArgumentManagerTemplate.builder().add("type", "Typ", "Der Typ der Antibeere", Translation.Type.TYPE)
                .setExample("!berry Rock")
                .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val ger = e.arguments.getTranslation("type").translation
        val g: String
        val engl: String
        when (ger) {
            "Feuer" -> {
                g = "Koakobeere"
                engl = "Occa Berry"
            }

            "Wasser" -> {
                g = "Foepasbeere"
                engl = "Passho Berry"
            }

            "Elektro" -> {
                g = "Kerzalbeere"
                engl = "Wacan Berry"
            }

            "Pflanze" -> {
                g = "Grindobeere"
                engl = "Rindo Berry"
            }

            "Eis" -> {
                g = "Kiroyabeere"
                engl = "Yache Berry"
            }

            "Kampf" -> {
                g = "Rospelbeere"
                engl = "Chople Berry"
            }

            "Gift" -> {
                g = "Grarzbeere"
                engl = "Kebia Berry"
            }

            "Boden" -> {
                g = "Schukebeere"
                engl = "Shuca Berry"
            }

            "Flug" -> {
                g = "Kobabeere"
                engl = "Coba Berry"
            }

            "Psycho" -> {
                g = "Pyapabeere"
                engl = "Payapa Berry"
            }

            "Käfer" -> {
                g = "Tanigabeere"
                engl = "Tanga Berry"
            }

            "Gestein" -> {
                g = "Chiaribeere"
                engl = "Charti Berry"
            }

            "Geist" -> {
                g = "Zitarzbeere"
                engl = "Kasib Berry"
            }

            "Drache" -> {
                g = "Terirobeere"
                engl = "Haban Berry"
            }

            "Unlicht" -> {
                g = "Burleobeere"
                engl = "Colbur Berry"
            }

            "Stahl" -> {
                g = "Babiribeere"
                engl = "Babiri Berry"
            }

            "Normal" -> {
                g = "Latchibeere"
                engl = "Chilan Berry"
            }

            "Fee" -> {
                g = "Hibisbeere"
                engl = "Roseli Berry"
            }

            else -> {
                g = "Es ist ein unbekannter Fehler aufgetreten! Bitte kontaktiere Flo mit `!flohelp <Nachricht>` !"
                engl = ""
            }
        }
        e.reply("Deutsch: $g\nEnglisch: $engl")
    }
}