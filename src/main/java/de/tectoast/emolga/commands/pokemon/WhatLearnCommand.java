package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class WhatLearnCommand extends Command {

    public final HashMap<String, String> map = new HashMap<>();

    public WhatLearnCommand() {
        super("whatlearn", "Zeigt an, welche Attacken ein Pokemon auf eine bestimme Art lernen kann", CommandCategory.Pokemon);
        map.put("Level", "L");
        map.put("TM", "M");
        map.put("Tutor", "T");
        map.put("Zucht", "E");
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("form", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                        SubCommand.of("Alola"), SubCommand.of("Galar")
                ), true)
                .addEngl("mon", "Pokemon", "Das Pokemon", Translation.Type.POKEMON)
                .add("type", "Art", "Die Lernart", ArgumentManagerTemplate.Text.of(
                        SubCommand.of("Level"),
                        SubCommand.of("TM"),
                        SubCommand.of("Tutor"),
                        SubCommand.of("Zucht")
                ).setMapper(map::get))
                .add("gen", "Generation", "Die Generation, sonst Gen 8", ArgumentManagerTemplate.Number.range(1, 8), true)
                .setExample("!howlearn Emolga Level 6")
                .build());
        aliases.add("howlearn");
    }

    public static boolean banane(JSONObject learnset, int gen, String type, HashMap<Integer, List<String>> levels, LinkedList<String> list) {
        for (String s : learnset.keySet()) {
            ArrayList<String> arr = learnset.getJSONArray(s).toList().stream().map(o -> ((String) o)).collect(Collectors.toCollection(ArrayList::new));
            if (arr.stream().anyMatch(t -> t.startsWith(String.valueOf(gen)) && t.contains(type))) {
                String name = getGerNameNoCheck(s);
                if (type.equals("L")) {
                    arr.stream().filter(str -> str.startsWith(gen + "L")).map(str -> str.substring(str.indexOf('L') + 1)).map(Integer::parseInt).forEach(i -> {
                        if (!levels.containsKey(i)) levels.put(i, new ArrayList<>());
                        List<String> l = levels.get(i);
                        l.add(name);
                    });
                } else
                    list.add(name);
            }
        }
        return levels.size() > 0;
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        String mon = toSDName(args.getTranslation("mon").getTranslation() + args.getOrDefault("form", ""));
        String type = args.getText("type");
        int gen = args.getOrDefault("gen", 8);
        JSONObject learnset = getLearnsetJSON(getModByGuild(e)).getJSONObject(mon).getJSONObject("learnset");
        LinkedList<String> list = new LinkedList<>();
        HashMap<Integer, List<String>> levels = new HashMap<>();
        boolean b = banane(learnset, gen, type, levels, list);
        if (!b) banane(learnset, --gen, type, levels, list);
        String send;
        if (type.equals("L")) {
            StringBuilder str = new StringBuilder();
            for (int i = 0; i <= 100; i++) {
                if (levels.containsKey(i)) {
                    levels.get(i).sort(null);
                    str.append("L").append(i).append(": ").append(String.join(", ", levels.get(i))).append("\n");
                }
            }
            send = str.toString();
        } else {
            if (list.isEmpty()) {
                e.reply("Dieses Pokemon kann in Generation " + gen + " auf diese Art keine Attacken lernen!");
                return;
            }
            ArrayList<String> set = new ArrayList<>(new HashSet<>(list));
            set.sort(null);
            send = String.join("\n", set);
        }
        e.reply(new EmbedBuilder().setColor(Color.CYAN).setDescription(send).setTitle("Attacken").build());
        if (e.getMember().getIdLong() == 598199247124299776L) e.reply("Hier noch ein Keks für dich :3");
    }
}
