package de.tectoast.emolga.commands.dexquiz;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.utils.ConfigManager;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum DexQuizTip {
    EINTRAGS_GENERATION("Preis für die Eintrags-Generation", "Die Generation, aus welcher der Pokedex-Eintrag stammt", 5, t -> "Der Eintrag stammt aus **%s**!".formatted(t.entryGen())),
    POKEMON_GENERATION("Preis für die Generation des Pokemons", "Die Generation, aus der das Pokemon stammt", 20, t -> "Das Pokemon stammt aus **Generation %d**!".formatted(Command.getGenerationFromDexNumber(t.monData().getInt("num")))),
    POKEMON_SINGLETYPE("Preis für einen Typen des Pokemons", "Ein Typ des Pokemons", 25, t -> {
        List<String> types = t.monData().getStringList("types");
        return "Das Pokemon besitzt mindestens folgenden Typen: **%s**".formatted(Command.getGerNameNoCheck(types.get(random().nextInt(types.size()))));
    }),
    POKEMON_BOTHTYPES("Preis für das Typing des Pokemons", "Der Typ/Die Typen des Pokemons", 40, t -> "Das Pokemon besitzt folgende Typen: **%s**".formatted(t.monData().getStringList("types").stream()
            .map(Command::getGerNameNoCheck).collect(Collectors.joining(" ")))),
    EIGRUPPE("Preis für die Eigruppe des Pokemons", "Die Eigruppe(n) des Pokemons", -1, t -> "Das Pokemon besitzt folgende Eigruppen: **%s**".formatted(t.monData().getStringList("eggGroups").stream()
            .map(s -> "e" + s)
            .map(Command::getGerNameNoCheck).collect(Collectors.joining(" ")))),
    ANFANGSBUCHSTABE("Preis für den Anfangsbuchstaben des Pokemons", "Der Anfangsbuchstabe des Pokemons auf deutsch und englisch", 10, t -> "Anfangsbuchstabe auf Deutsch: %s\nAnfangsbuchstabe auf Englisch: %s".formatted(t.name().charAt(0), t.englName().charAt(0)));
    private static final Random random = new Random();
    private final String configLabel;
    private final String description;
    private final int defaultPrice;
    private final Function<TipData, String> tipFunction;

    DexQuizTip(String configLabel, String description, int defaultPrice, Function<TipData, String> tipFunction) {
        this.configLabel = configLabel;
        this.description = description;
        this.defaultPrice = defaultPrice;
        this.tipFunction = tipFunction;
    }

    private static Random random() {
        return random;
    }

    public static TextInput[] buildActionRows() {
        return Arrays.stream(values()).map(d -> TextInput.create(
                d.name(), d.configLabel, TextInputStyle.SHORT
        ).setPlaceholder(String.valueOf(d.defaultPrice)).setRequired(false).build()).toArray(TextInput[]::new);
    }

    public static List<Command.SubCommand> buildSubcommands() {
        return Arrays.stream(values()).map(t -> Command.SubCommand.of(t.name().toLowerCase(), t.description)).toList();
    }

    public static List<MessageEmbed.Field> buildEmbedFields(long gid) {
        return Arrays.stream(values())
                .filter(dt -> (int) ConfigManager.DEXQUIZ.getValue(gid, dt.name()) >= 0)
                .map(dt -> new MessageEmbed.Field(dt.configLabel, ConfigManager.DEXQUIZ.getValue(gid, dt.name()).toString(), false)).toList();
    }

    public Function<TipData, String> getTipFunction() {
        return tipFunction;
    }

    public int getDefaultPrice() {
        return defaultPrice;
    }

    public record TipData(String name, String englName, String entryGen, JSONObject monData) {
    }
}