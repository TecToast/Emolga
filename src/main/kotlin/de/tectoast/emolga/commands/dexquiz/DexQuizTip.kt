package de.tectoast.emolga.commands.dexquiz

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.Command.SubCommand
import de.tectoast.emolga.utils.ConfigManager
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

enum class DexQuizTip(
    private val configLabel: String,
    private val description: String,
    val defaultPrice: Int,
    val tipFunction: Function<TipData, String>,
) {
    EINTRAGS_GENERATION(
        "Preis für die Eintrags-Generation",
        "Die Generation, aus welcher der Pokedex-Eintrag stammt",
        5,
        Function { t: TipData -> "Der Eintrag stammt aus **${t.entryGen}**!" }),
    POKEMON_GENERATION(
        "Preis für die Generation des Pokemons",
        "Die Generation, aus der das Pokemon stammt",
        20,
        Function { t: TipData ->
            "Das Pokemon stammt aus **Generation ${Command.getGenerationFromDexNumber(t.monData.getInt("num"))}**!"
        }),
    POKEMON_SINGLETYPE("Preis für einen Typen des Pokemons", "Ein Typ des Pokemons", 25, Function { t: TipData ->
        val types: List<String> = t.monData.getStringList("types")
        "Das Pokemon besitzt mindestens folgenden Typen: **${Command.getGerNameNoCheck(types[random().nextInt(types.size)])}**"
    }),
    POKEMON_BOTHTYPES(
        "Preis für das Typing des Pokemons",
        "Der Typ/Die Typen des Pokemons",
        40,
        Function { t: TipData ->
            "Das Pokemon besitzt folgende Typen: **${
                t.monData.getStringList("types").stream()
                    .map { Command.getGerNameNoCheck(it) }
                    .collect(Collectors.joining(" "))
            }**"
        }),
    EIGRUPPE("Preis für die Eigruppe des Pokemons", "Die Eigruppe(n) des Pokemons", -1, Function { t: TipData ->
        "Das Pokemon besitzt folgende Eigruppen: **${
            t.monData.getStringList("eggGroups").stream()
                .map { "e$it" }
                .map { Command.getGerNameNoCheck(it) }.collect(Collectors.joining(" "))
        }**"
    }),
    ANFANGSBUCHSTABE(
        "Preis für den Anfangsbuchstaben des Pokemons",
        "Der Anfangsbuchstabe des Pokemons auf deutsch und englisch",
        10,
        Function { t: TipData ->
            "Anfangsbuchstabe auf Deutsch: ${t.name[0]}\nAnfangsbuchstabe auf Englisch: ${t.englName[0]}"
        });

    class TipData(val name: String, val englName: String, val entryGen: String, val monData: JSONObject)
    companion object {
        private val random = Random()
        private fun random(): Random {
            return random
        }

        @JvmStatic
        fun buildActionRows(): Array<TextInput> {
            return Arrays.stream(values()).map { d: DexQuizTip ->
                TextInput.create(
                    d.name, d.configLabel, TextInputStyle.SHORT
                ).setPlaceholder(d.defaultPrice.toString()).setRequired(false).build()
            }.toArray { arrayOfNulls(it) }
        }

        fun buildSubcommands(): List<SubCommand> {
            return Arrays.stream(values())
                .map { t: DexQuizTip -> SubCommand.of(t.name.lowercase(Locale.getDefault()), t.description) }
                .toList()
        }

        @JvmStatic
        fun buildEmbedFields(gid: Long): List<MessageEmbed.Field> {
            return Arrays.stream(values())
                .filter { dt: DexQuizTip -> ConfigManager.DEXQUIZ.getValue(gid, dt.name) as Int >= 0 }
                .map { dt: DexQuizTip ->
                    MessageEmbed.Field(
                        dt.configLabel,
                        ConfigManager.DEXQUIZ.getValue(gid, dt.name).toString(),
                        false
                    )
                }
                .toList()
        }
    }
}