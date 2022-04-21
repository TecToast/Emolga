package de.tectoast.emolga.utils;

import de.tectoast.emolga.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jsolf.JSONObject;

import java.awt.*;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.getGerNameNoCheck;

public record TypicalSets(JSONObject json) {
    private static TypicalSets instance;

    public static void init(JSONObject o) {
        if (instance == null) instance = new TypicalSets(o);
    }

    public static TypicalSets getInstance() {
        return instance;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public void add(String pokemon, Collection<String> movesList, Optional<String> item, Optional<String> ability) {
        JSONObject mon = json.createOrGetJSON(pokemon);
        mon.increment("uses");
        movesList.forEach(m -> mon.createOrGetJSON("moves").increment(getGerNameNoCheck(m)));
        item.ifPresent(s -> mon.createOrGetJSON("items").increment(getGerNameNoCheck(s)));
        ability.ifPresent(s -> mon.createOrGetJSON("abilities").increment(getGerNameNoCheck(s)));
    }

    public synchronized void save() {
        Command.save(json, "typicalsets.json");
    }

    public MessageEmbed buildPokemon(String pokemon) {
        if (!json.has(pokemon)) {
            return new EmbedBuilder().setTitle("Dieses Pokemon ist noch nicht in den TypicalSets erfasst!").setColor(Color.RED).build();
        }
        JSONObject mon = json.getJSONObject(pokemon);
        double uses = mon.getInt("uses");
        return new EmbedBuilder().addField("Attacken", mon.optJSONObject("moves", new JSONObject()).toMap().entrySet().stream().map(e -> {
            String usesStr = String.valueOf((int) e.getValue() / uses * 100f);
            return e.getKey() + ": " + usesStr.substring(0, Math.min(usesStr.length(), 5)) + "%";
        }).limit(5).collect(Collectors.joining("\n")), true).addField("Items", mon.optJSONObject("items", new JSONObject()).toMap().entrySet().stream().map(e -> {
            String itemsStr = String.valueOf((int) e.getValue() / uses * 100f);
            return e.getKey() + ": " + itemsStr.substring(0, Math.min(itemsStr.length(), 5)) + "%";
        }).limit(5).collect(Collectors.joining("\n")), true).addField("Fähigkeiten", mon.optJSONObject("abilities", new JSONObject()).toMap().entrySet().stream().map(e -> {
            String abilitiesStr = String.valueOf((int) e.getValue() / uses * 100f);
            return e.getKey() + ": " + abilitiesStr.substring(0, Math.min(abilitiesStr.length(), 5)) + "%";
        }).limit(5).collect(Collectors.joining("\n")), true).setColor(Color.CYAN).setTitle(pokemon).build();
    }
}
