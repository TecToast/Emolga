package de.Flori.Commands.Pokemon;


import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class DataCommand extends Command {
    public DataCommand() {
        super("data", "`!data [Shiny] <Pokemon|Attacke|Fähigkeit|Item>` Zeigt Informationen über diese Sache", CommandCategory.Pokemon);
        aliases.add("dt");
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        try {
            String[] args = msg.split(" ");
            if (args.length < 2) {
                tco.sendMessage("Syntax: !data <Pokémon Name>").queue();
                return;
            }
            String name = msg.startsWith("!dt") ? msg.substring(4) : msg.substring(6);
            if (msg.toLowerCase().contains("shiny")) {
                name = name.substring(6);
            }
            String gerName = getGerName(name);
            if (gerName.split(";")[0].equals("pkmn")) {
                try {
                    JSONObject json = getDataJSON();
                    String monname = gerName.split(";")[1];
                    EmbedBuilder builder = new EmbedBuilder();
                    JSONObject mon = json.getJSONObject(monname.toLowerCase());
                    builder.addField("Englisch", Command.getEnglName(monname), true);
                    builder.addField("Dex", String.valueOf(mon.getInt("num")), true);
                    String gender;
                    if (mon.has("genderRatio")) {
                        JSONObject gen = mon.getJSONObject("genderRatio");
                        gender = gen.getDouble("M") * 100 + "% ♂ " + gen.getDouble("F") * 100 + "% ♀";
                    } else if (mon.has("gender")) {
                        gender = mon.getString("gender").equals("M") ? "100% ♂" : (mon.getString("gender").equals("F") ? "100% ♀" : "Unbekannt");
                    } else gender = "50% ♂ 50% ♀";
                    builder.addField("Geschlecht", gender, true);
                    String egg = mon.getJSONArray("eggGroups").getString(0);
                    List<JSONObject> list = getAllForms(monname);
                    if (monname.equalsIgnoreCase("amigento") || monname.equalsIgnoreCase("arceus")) {
                        builder.addField("Typen", "Normal", false);
                    } else {
                        HashMap<String, ArrayList<String>> types = new HashMap<>();
                        for (JSONObject obj : list) {
                            System.out.println(obj);
                            String type = obj.getJSONArray("types").toList().stream().map(o -> (String) o).collect(Collectors.joining(" "));

                            if (types.containsKey(type)) types.get(type).add(obj.getString("forme"));
                            else
                                types.put(type, new ArrayList<>(Collections.singletonList(obj.has("forme") ? obj.getString("forme") : "Normal")));
                        }
                        StringBuilder type = new StringBuilder();
                        if (types.size() == 1) type.append(types.keySet().toArray(new String[1])[0]);
                        else
                            for (String s : types.keySet().stream().sorted((o1, o2) -> {
                                ArrayList<String> l1 = types.get(o1);
                                ArrayList<String> l2 = types.get(o2);
                                if (l1.contains("Normal")) return -1;
                                if (l2.contains("Normal")) return 1;
                                return l1.get(0).compareTo(l2.get(0));
                            }).collect(Collectors.toList())) {
                                ArrayList<String> l = types.get(s);
                                if (l.size() == 1 && l.contains("Normal")) type.append(s).append("\n");
                                else {
                                    type.append(s).append(" (").append(String.join(", ", l)).append(")\n");
                                }
                            }
                        builder.addField("Typen", type.toString(), false);
                    }
                    HashMap<Double, ArrayList<String>> heights = new HashMap<>();
                    for (JSONObject obj : list) {
                        double d = obj.getDouble("heightm");
                        if (heights.containsKey(d)) heights.get(d).add(obj.getString("forme"));
                        else
                            heights.put(d, new ArrayList<>(Collections.singletonList(obj.has("forme") ? obj.getString("forme") : "Normal")));
                    }
                    StringBuilder height = new StringBuilder();
                    if (heights.size() == 1) {
                        height.append(heights.keySet().toArray(new Double[0])[0]).append(" m");
                    } else {
                        for (Double d : heights.keySet().stream().sorted((o1, o2) -> {
                            ArrayList<String> l1 = heights.get(o1);
                            ArrayList<String> l2 = heights.get(o2);
                            if (l1.contains("Normal")) return -1;
                            if (l2.contains("Normal")) return 1;
                            return l1.get(0).compareTo(l2.get(0));
                        }).collect(Collectors.toList())) {
                            ArrayList<String> l = heights.get(d);
                            if (l.size() == 1 && l.contains("Normal")) height.append(d).append(" m\n");
                            else {
                                height.append(d).append(" m (").append(String.join(", ", l)).append(")\n");
                            }
                        }
                    }
                    builder.addField("Größe", height.toString(), true);
                    HashMap<Double, ArrayList<String>> weights = new HashMap<>();
                    for (JSONObject obj : list) {
                        double d = obj.getDouble("weightkg");
                        if (weights.containsKey(d)) weights.get(d).add(obj.getString("forme"));
                        else
                            weights.put(d, new ArrayList<>(Collections.singletonList(obj.has("forme") ? obj.getString("forme") : "Normal")));
                    }
                    StringBuilder weight = new StringBuilder();
                    if (weights.size() == 1) {
                        weight.append(weights.keySet().toArray(new Double[0])[0]).append(" kg");
                    } else {
                        for (Double d : weights.keySet().stream().sorted((o1, o2) -> {
                            ArrayList<String> l1 = weights.get(o1);
                            ArrayList<String> l2 = weights.get(o2);
                            if (l1.contains("Normal")) return -1;
                            if (l2.contains("Normal")) return 1;
                            return l1.get(0).compareTo(l2.get(0));
                        }).collect(Collectors.toList())) {
                            ArrayList<String> l = weights.get(d);
                            if (l.size() == 1 && l.contains("Normal")) weight.append(d).append(" kg\n");
                            else {
                                weight.append(d).append(" kg (").append(String.join(", ", l)).append(")\n");
                            }
                        }
                    }
                    builder.addField("Gewicht", weight.toString(), true);
                    builder.addField("Eigruppe", mon.getJSONArray("eggGroups").toList().stream().map(o -> (String) o).collect(Collectors.joining(", ")), true);
                    String baseforme = mon.has("baseForme") ? mon.getString("baseForme") : "Normal";
                    if (monname.equalsIgnoreCase("amigento") || monname.equalsIgnoreCase("arceus")) {
                        builder.addField("Fähigkeiten", monname.equalsIgnoreCase("amigento") ? "Alpha-System" : "Variabilität", false);
                    } else {
                        HashMap<String, ArrayList<String>> abis = new HashMap<>();
                        for (JSONObject obj : list) {
                            JSONObject o = obj.getJSONObject("abilities");
                            if (o.has("0")) {
                                String a = o.getString("0");
                                if (!abis.containsKey(a))
                                    abis.put(a, new ArrayList<>(Collections.singletonList(obj.has("forme") ? obj.getString("forme") : baseforme)));
                                else abis.get(a).add(obj.has("forme") ? obj.getString("forme") : baseforme);
                            }
                            if (o.has("1")) {
                                String a = o.getString("1");
                                if (!abis.containsKey(a))
                                    abis.put(a, new ArrayList<>(Collections.singletonList(obj.has("forme") ? obj.getString("forme") : baseforme)));
                                else abis.get(a).add(obj.has("forme") ? obj.getString("forme") : baseforme);
                            }
                            if (o.has("H")) {
                                String a = o.getString("H");
                                if (!abis.containsKey(a))
                                    abis.put(a, new ArrayList<>(Collections.singletonList((obj.has("forme") ? obj.getString("forme") : baseforme) + " VF")));
                                else abis.get(a).add((obj.has("forme") ? obj.getString("forme") : baseforme) + " VF");
                            }
                        }
                        StringBuilder abi = new StringBuilder();
                        if (abis.size() == 1) abi.append(abis.keySet().toArray(new String[0])[0]);
                        else
                            for (String s : abis.keySet().stream().sorted((o1, o2) -> {
                                ArrayList<String> l1 = abis.get(o1);
                                ArrayList<String> l2 = abis.get(o2);
                                if (l1.contains(baseforme) && !l2.contains(baseforme)) return -1;
                                if (!l1.contains(baseforme) && l2.contains(baseforme)) return 1;
                                if (l1.stream().anyMatch(s -> s.contains(baseforme + " VF"))) return -1;
                                if (l2.stream().anyMatch(s -> s.contains(baseforme + " VF"))) return 1;
                                return l1.get(0).compareTo(l2.get(0));
                            }).collect(Collectors.toList())) {
                                ArrayList<String> l = abis.get(s);
                                System.out.println("l = " + l);
                                if (l.size() == 1 && l.contains("Normal")) abi.append(s).append("\n");
                                else {
                                    if (list.size() == 1) {
                                        abi.append(s).append(" (").append(String.join(", ", l).replace("Normal ", "").replace("Normal", "")).append(")\n");
                                    } else abi.append(s).append(" (").append(String.join(", ", l)).append(")\n");
                                }
                            }
                            String str = abi.toString();
                            if(!str.contains("Alola VF") && !str.contains("Galar VF")) str = str.replace("Normal VF", "VF");
                            builder.addField("Fähigkeiten", str, false);
                    }
                    if (monname.equalsIgnoreCase("amigento") || monname.equalsIgnoreCase("arceus")) {
                        builder.addField(monname.equalsIgnoreCase("amigento") ? "Amigento" : "Arceus", monname.equalsIgnoreCase("amigento") ? "KP: 95\n" + "Atk: 95\n" + "Def: 95\n" + "SpAtk: 95\n" + "SpDef: 95\n" + "Init: 95\n" + "Summe: 570"
                                : "KP: 120\n" + "Atk: 120\n" + "Def: 120\n" + "SpAtk: 120\n" + "SpDef: 120\n" + "Init: 120\n" + "Summe: 720", false);
                    } else {
                        HashMap<String, ArrayList<String>> stat = new HashMap<>();
                        for (JSONObject obj : list) {
                            JSONObject stats = obj.getJSONObject("baseStats");
                            int kp = stats.getInt("hp");
                            int atk = stats.getInt("atk");
                            int def = stats.getInt("def");
                            int spa = stats.getInt("spa");
                            int spd = stats.getInt("spd");
                            int spe = stats.getInt("spe");
                            String str = "KP: " + kp + "\nAtk: " + atk + "\nDef: " + def + "\nSpAtk: " + spa
                                    + "\nSpDef: " + spd + "\nInit: " + spe + "\nSumme: " + (kp + atk + def + spa + spd + spe);
                            String toadd = obj.getString("name");
                            if(toadd.endsWith("-Alola")) toadd = "Alola-" + toadd.substring(0, toadd.length() - 6);
                            if(toadd.endsWith("-Galar")) toadd = "Galar-" + toadd.substring(0, toadd.length() - 6);
                            if (stat.containsKey(str)) stat.get(str).add(toadd);
                            else stat.put(str, new ArrayList<>(Collections.singletonList(toadd)));
                        }
                        for (String s : stat.keySet().stream().sorted(Comparator.comparing(o -> stat.get(o).get(0).length())).collect(Collectors.toList())) {
                            builder.addField(String.join(", ", stat.get(s)), s, true);
                        }
                    }
                    if (msg.toLowerCase().contains("shiny"))
                        builder.setImage(getShinySpriteJSON().getString(String.valueOf(mon.getInt("num"))));
                    else builder.setImage(getSpriteJSON().getString(String.valueOf(mon.getInt("num"))));
                    builder.setTitle(monname);
                    builder.setColor(Color.CYAN);
                    tco.sendMessage(builder.build()).queue();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                /*EmbedBuilder builder = new EmbedBuilder();
                name = str.split(";")[1];
                JSONObject data = getWikiJSON().getJSONObject("pkmndata").getJSONObject(name);
                String dexnumber = data.getString("dex");
                builder.addField("English", getEnglName(name), true);
                builder.addField("Dex", dexnumber, true);
                builder.addField("Gender", data.getString("gender"), true);
                builder.addField("Types", data.getString("types"), false);
                builder.addField("Height", data.getString("height"), true);
                builder.addField("Weight", data.getString("weight"), true);
                builder.addField("Egg Group", data.getString("egggroup"), true);
                builder.addField("Abilities", data.getString("abilities"), false);
                JSONObject allstats = data.getJSONObject("stats");
                ArrayList<String> set = new ArrayList<>(allstats.keySet());
                set.sort(Comparator.comparingInt(s -> allstats.getJSONObject(s).getInt("summe")));
                for (String s : set) {
                    JSONObject stats = allstats.getJSONObject(s);
                    builder.addField(s,
                            "KP: " + stats.getInt("kp")
                                    + "\nAtk: " + stats.getInt("atk")
                                    + "\nDef: " + stats.getInt("def")
                                    + "\nSpAtk: " + stats.getInt("spatk")
                                    + "\nSpDef: " + stats.getInt("spdef")
                                    + "\nInit: " + stats.getInt("init")
                                    + "\nSumme: " + stats.getInt("summe"), true);
                }
                /*try {
                    try {
                        try {
                            d = Jsoup.connect("https://www.pokewiki.de/" + name).get();
                        } catch (Exception ex) {
                            d = Jsoup.connect("https://www.pokewiki.de/" + eachWordUpperCase(name)).get();
                        }
                    } catch (IOException ioException) {
                        tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
                        ioException.printStackTrace();
                        return;
                    }

                    // Dex

                    if (msg.toLowerCase().contains("shiny")) {
                        boolean b = false;
                        for (Element element : d.select("img")) {
                            if (b) break;
                            Elements ele = element.getElementsByAttribute("alt");
                            for (Element eleme : ele) {
                                if (eleme.attr("alt").toLowerCase().contains("schillernd")) {
                                    if (msg.toLowerCase().contains("gigadynamax")) {
                                        if (eleme.attr("alt").contains(dexnumber + "g1")) {
                                            builder.setImage("https://www.pokewiki.de" + eleme.attr("src"));
                                            b = true;
                                            break;
                                        }
                                    } else {
                                        builder.setImage("https://www.pokewiki.de" + eleme.attr("src"));
                                        b = true;
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        if (msg.toLowerCase().contains("gigadynamax")) {
                            boolean b = false;
                            for (Element element : d.select("img")) {
                                if (b) break;
                                Elements ele = element.getElementsByAttribute("alt");
                                for (Element eleme : ele) {
                                    if (eleme.attr("alt").contains(dexnumber + "g1") && !eleme.attr("alt").toLowerCase().contains("schillernd")) {
                                        builder.setImage("https://www.pokewiki.de" + eleme.attr("src"));
                                        b = true;
                                        break;
                                    }
                                }
                            }
                        } else
                            builder.setImage("https://www.pokewiki.de" + Jsoup.connect("https://www.pokewiki.de/Datei:Sugimori_" + dexnumber + ".png").get().select("img").first().attr("src"));
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                builder.setImage(Command.getSpriteJSON().getString(String.valueOf(Integer.parseInt(dexnumber))));
                if (!builder.isEmpty()) {
                    builder.setTitle(name);
                    builder.setColor(Color.CYAN);
                    tco.sendMessage(builder.build()).queue();
                } else tco.sendMessage("Dieses Pokémon existiert nicht!").queue();*/
                return;
            }

            if (gerName.split(";")[0].equals("atk")) {
                name = gerName.split(";")[1];
                JSONObject data = getWikiJSON().getJSONObject("atkdata").getJSONObject(name);
                String p = data.getString("power");
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle(name)
                        .addField("English", getEnglName(name), true)
                        .addField("Power", p, true)
                        .addField("Dyna-Power", data.getString("dynapower"), true)
                        .addField("Accuracy", data.getString("accuracy"), true)
                        .addField("Category", data.getString("category"), true)
                        .addField("AP", data.getString("ap"), true)
                        .addField("Type", data.getString("type"), true)
                        .addField("Priority", data.getString("prio"), true)
                        .setColor(Color.CYAN)
                        .setDescription(data.getString("description"));
                if (data.getString("category").equals("Status")) {
                    builder.addField("Z-Effect", data.has("zpower") ? data.getString("zpower") : "Nichts", true);
                } else {
                    String zpower = null;
                    if (data.has("zpower")) zpower = String.valueOf(data.getInt("zpower"));
                    else {
                        if (p.equalsIgnoreCase("K.O.")) zpower = "180";
                        if (p.equalsIgnoreCase("variiert")) {
                            zpower = "variiert";
                        } else {
                            int power = Integer.parseInt(p);
                            if (power <= 55) zpower = "100";
                            else if (power >= 60 && power <= 65) zpower = "120";
                            else if (power >= 70 && power <= 75) zpower = "140";
                            else if (power >= 80 && power <= 85) zpower = "160";
                            else if (power >= 90 && power <= 95) zpower = "175";
                            else if (power == 100) zpower = "180";
                            else if (power == 110) zpower = "185";
                            else if (power == 120) zpower = "190";
                            else if (power == 130) zpower = "195";
                            else if (power >= 140) zpower = "200";
                        }
                    }
                    if (zpower == null) sendToMe("Fehler bei Z-" + name + "!");
                    builder.addField("Z-Power", zpower, true);
                }
                tco.sendMessage(builder.build()).queue();
                return;
            }
            Document d;
            try {
                try {
                    d = Jsoup.connect("https://www.pokewiki.de/" + name).get();
                } catch (Exception ex) {
                    d = Jsoup.connect("https://www.pokewiki.de/" + eachWordUpperCase(name)).get();
                }
            } catch (IOException ioException) {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
                ioException.printStackTrace();
                return;
            }

            // Dex
            if (d.select("span[lang=\"en\"]").text().length() > 2) {
                EmbedBuilder builder = new EmbedBuilder();
                String gerNameWiki = d.select("h1").first().text();
                String s;
                if (!name.equalsIgnoreCase(gerNameWiki)) s = "Englisch: " + eachWordUpperCase(name) + "\n";
                else s = "Englisch: " + d.select("span[lang=\"en\"]").text() + "\n";
                if (name.equalsIgnoreCase("Verborgene Faust")) s = "Englisch: Unseen Fist\n";
                if (name.equalsIgnoreCase("Unseen Fist")) s = "Englisch: Unseen Fist\n";
                gerNameWiki = d.select("p").get(d.select("p").get(0).text().length() == 0 ? 2 : 1).text();
                if (gerNameWiki.startsWith("Giga-") || d.text().contains("ist eine Z-Attacke"))
                    gerNameWiki = d.select("p").get(2).text();
                if (gerNameWiki.length() <= 2) gerNameWiki = "Keine Daten vorhanden";
                builder.setTitle(d.select("h1").first().text()).setColor(Color.CYAN).setDescription(s + gerNameWiki);
                tco.sendMessage(builder.build()).queue();
            } else {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
                System.out.println("Text");
                System.out.println(d.select("span[lang=\"en\"]").text());
            }
        } catch (Exception ex) {
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
            ex.printStackTrace();
        }
    }
}
