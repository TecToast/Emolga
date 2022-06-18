package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONObject;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class WeaknessCommand extends Command {

    private final Map<String, List<String>> immunities = new HashMap<>();
    private final Map<String, Map<String, Integer>> resistances = new HashMap<>();

    public WeaknessCommand() {
        super("weakness", "Zeigt die Schwächen und Resistenzen eines Pokémons an", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("regform", "Form", "", ArgumentManagerTemplate.Text.of(
                        SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
                ), true)
                .add("stuff", "Pokemon", "Pokemon/Item/Whatever", Translation.Type.POKEMON)
                .add("form", "Sonderform", "Sonderform, bspw. `Heat` bei Rotom", ArgumentManagerTemplate.Text.any(), true)
                .setExample("!weakness Primarina")
                .build());
        immunities.put("Electric", Arrays.asList("Elektro", "Lightning Rod", "Volt Absorb", "Motor Drive"));
        immunities.put("Fire", Arrays.asList("Feuer", "Flash Fire"));
        immunities.put("Water", Arrays.asList("Wasser", "Water Absorb", "Storm Drain", "Dry Skin"));
        immunities.put("Ground", Arrays.asList("Boden", "Levitate"));
        immunities.put("Grass", Arrays.asList("Pflanze", "Sap Sipper"));
        resistances.put("Fire", Map.of("Fluffy", 1, "Thick Fat", -1));
        resistances.put("Ice", Map.of("Thick Fat", -1));
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        Translation gerName = args.getTranslation("stuff");
        String name = gerName.getTranslation();
        JSONObject mon = getDataJSON().optJSONObject(toSDName(gerName.getOtherLang() + args.getOrDefault("regform", "") + args.getOrDefault("form", "") + gerName.getForme()));
        JSONObject abijson = mon.getJSONObject("abilities");
        List<String> abilities = abijson.keySet().stream().map(abijson::getString).collect(Collectors.toList());
        boolean oneabi = abilities.size() == 1;
        if (mon == null) {
            e.reply(name + " besitzt diese Form nicht!");
            return;
        }
        String[] types = mon.getStringList("types").toArray(String[]::new);
        Set<String> x2 = new LinkedHashSet<>();
        Set<String> x05 = new LinkedHashSet<>();
        Set<String> x0 = new LinkedHashSet<>();
        HashMap<String, String> immuneAbi = new HashMap<>();
        HashMap<String, EffectivenessChangeText> changeAbi = new HashMap<>();
        for (String type : getTypeJSON().keySet()) {
            if (getImmunity(type, types))
                x0.add(((Translation) Translation.Type.TYPE.validate(type, Translation.Language.GERMAN, "default")).getTranslation());
            else {
                if (oneabi && immunities.containsKey(type) && immunities.get(type).contains(abilities.get(0))) {
                    x0.add(immunities.get(type).get(0) + " **(wegen " + abilities.get(0) + ")**");
                } else {
                    String abii = checkAbiImmunity(type, abilities);
                    if (abii != null && !abii.isBlank()) {
                        immuneAbi.put(abii, ((Translation) Translation.Type.TYPE.validate(type, Translation.Language.GERMAN, "default")).getTranslation());
                    }
                    int typeMod = getEffectiveness(type, types);
                    if (typeMod != 0) {
                        String t = ((Translation) Translation.Type.TYPE.validate(type, Translation.Language.GERMAN, "default")).getTranslation();
                        switch (typeMod) {
                            case 1 -> x2.add(t);
                            case 2 -> x2.add("**" + t + "**");
                            case -1 -> x05.add(t);
                            case -2 -> x05.add("**" + t + "**");
                        }
                    }
                    List<EffectivenessChangeNum> pl = checkAbiChanges(type, abilities);
                    for (EffectivenessChangeNum p : pl) {
                        int modified = typeMod + p.value();
                        String abi = p.ability();
                        if (modified != typeMod && !abi.isBlank()) {
                            String t = ((Translation) Translation.Type.TYPE.validate(type, Translation.Language.GERMAN, "default")).getTranslation();
                            switch (modified) {
                                case 2 -> changeAbi.put(t, new EffectivenessChangeText(abi, "vierfach-effektiv"));
                                case 1 -> changeAbi.put(t, new EffectivenessChangeText(abi, "sehr effektiv"));
                                case 0 -> changeAbi.put(t, new EffectivenessChangeText(abi, "neutral-effektiv"));
                                case -1 -> changeAbi.put(t, new EffectivenessChangeText(abi, "resistiert"));
                                case -2 -> changeAbi.put(t, new EffectivenessChangeText(abi, "vierfach-resistiert"));
                            }
                        }
                    }
                }
            }
        }
        e.reply("**" + name + "**:\nSchwächen: " + String.join(", ", x2) + "\nResistenzen: " + String.join(", ", x05)
                + (x0.size() > 0 ? "\nImmunitäten: " + String.join(", ", x0) : "")
                + "\n" + immuneAbi.keySet().stream().map(Command::getGerNameNoCheck).map(s -> "Wenn " + name + " **" + s + "** hat, wird der Typ **" + immuneAbi.get(s) + "** immunisiert.").collect(Collectors.joining("\n"))
                + "\n" + changeAbi.keySet().stream().map(Command::getGerNameNoCheck).map(s -> "Wenn " + name + " **" + changeAbi.get(s).ability() + "** hat, wird der Typ **" + s + " " + changeAbi.get(s).value() + "**.").collect(Collectors.joining("\n"))
        );
    }

    private static boolean getImmunity(String type, String... against) {
        if (against.length > 1) {
            for (String s : against) {
                if (getImmunity(type, s)) return true;
            }
            return false;
        }
        return getTypeJSON().getJSONObject(against[0]).getJSONObject("damageTaken").getInt(type) == 3;
    }

    private List<EffectivenessChangeNum> checkAbiChanges(String type, List<String> abilities) {
        if (!resistances.containsKey(type)) return Collections.emptyList();
        Map<String, Integer> l = resistances.get(type);
        return l.keySet().stream().filter(abilities::contains).map(s -> new EffectivenessChangeNum(s, l.get(s))).collect(Collectors.toList());
    }

    private record EffectivenessChangeNum(String ability, int value) {
    }

    private record EffectivenessChangeText(String ability, String value) {
    }

    public static int getEffectiveness(String type, String... against) {
        int totalTypeMod = 0;
        if (against.length > 1) {
            for (String s : against) {
                totalTypeMod += getEffectiveness(type, s);
            }
            return totalTypeMod;
        }
        JSONObject typeData = getTypeJSON().optJSONObject(against[0]);
        if (typeData == null) return 0;
        return switch (typeData.getJSONObject("damageTaken").getInt(type)) {
            case 1 -> 1; // super-effective
            case 2 -> -1; // resist
            // in case of weird situations like Gravity, immunity is handled elsewhere
            default -> 0;
        };
    }

    private @Nullable String checkAbiImmunity(String type, List<String> abilities) {
        if (!immunities.containsKey(type)) return null;
        List<String> l = immunities.get(type);
        return l.stream().filter(abilities::contains).collect(Collectors.joining(" oder "));
    }
}
