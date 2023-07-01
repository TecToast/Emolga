package de.tectoast.emolga.commands.dexquiz

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.Command.Companion.getTypeGerName
import de.tectoast.emolga.commands.Command.SubCommand
import de.tectoast.emolga.utils.ConfigManager
import de.tectoast.emolga.utils.json.showdown.Pokemon
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle

enum class DexQuizTip(
    private val configLabel: String,
    private val description: String,
    val defaultPrice: Int,
    val tipFunction: suspend (TipData) -> String,
) {
    EINTRAGS_GENERATION("Preis für die Eintrags-Generation",
        "Die Generation, aus welcher der Pokedex-Eintrag stammt",
        5,
        { "Der Eintrag stammt aus **${it.entryGen}**!" }),
    POKEMON_GENERATION("Preis für die Generation des Pokemons", "Die Generation, aus der das Pokemon stammt", 20, {
        "Das Pokemon stammt aus **Generation ${Command.getGenerationFromDexNumber(it.monData.num)}**!"
    }),
    POKEMON_SINGLETYPE("Preis für einen Typen des Pokemons", "Ein Typ des Pokemons", 25, { td ->
        val types: List<String> = td.monData.types
        "Das Pokemon besitzt mindestens folgenden Typen: **${
            getTypeGerName(types.random())
        }**"
    }),
    POKEMON_BOTHTYPES("Preis für das Typing des Pokemons", "Der Typ/Die Typen des Pokemons", 40, { td ->
        "Das Pokemon besitzt folgende Typen: **${
            td.monData.types.joinToString(" ") { t ->
                runBlocking { getTypeGerName(t) }
            }
        }**"
    }),
    EIGRUPPE("Preis für die Eigruppe des Pokemons", "Die Eigruppe(n) des Pokemons", -1, { td ->
        "Das Pokemon besitzt folgende Eigruppen: **${
            td.monData.eggGroups.joinToString(" ") { "e${Command.getGerNameNoCheck(it)}" }
        }**"
    }),
    ANFANGSBUCHSTABE("Preis für den Anfangsbuchstaben des Pokemons",
        "Der Anfangsbuchstabe des Pokemons auf deutsch und englisch",
        10, {
            "Anfangsbuchstabe auf Deutsch: ${it.name[0]}\nAnfangsbuchstabe auf Englisch: ${it.englName[0]}"
        });

    class TipData(val name: String, val englName: String, val entryGen: String, val monData: Pokemon)
    companion object {
        fun buildActionRows(): List<TextInput> {
            return values().map {
                TextInput.create(
                    it.name, it.configLabel, TextInputStyle.SHORT
                ).setPlaceholder(it.defaultPrice.toString()).setRequired(false).build()
            }
        }

        fun buildSubcommands(): List<SubCommand> {
            return values().map { SubCommand.of(it.name.lowercase(), it.description) }
        }

        fun buildEmbedFields(gid: Long): List<MessageEmbed.Field> {
            return values()
                .asSequence()
                .filter { ConfigManager.DEXQUIZ.getValue(gid, it.name) as Int >= 0 }
                .map {
                    MessageEmbed.Field(
                        it.configLabel, ConfigManager.DEXQUIZ.getValue(gid, it.name).toString(), false
                    )
                }.toList()
        }
    }
}
