package de.tectoast.emolga.commands.pokemon;


import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONObject;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class DataCommand extends Command {
    public DataCommand() {
        super("data", "`!data [Shiny] <Pokemon|Attacke|Fähigkeit|Item>` Zeigt Informationen über diese Sache", CommandCategory.Pokemon);
        aliases.add("dt");
    }

    @Override
    public void process(CommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        try {
            String[] args = msg.split(" ");
            if (args.length < 2) {
                tco.sendMessage("Syntax: !data <Name>").queue();
                return;
            }
            String name = msg.startsWith("!dt") ? msg.substring(4) : msg.substring(6);
            if (msg.toLowerCase().contains("shiny")) {
                name = name.substring(6);
            }
            String mod = getModByGuild(e);
            String gerName = getGerName(name, mod);
            String objtype = gerName.split(";")[0];
            switch (objtype) {
                case "pkmn":
                    try {
                        JSONObject json = getDataJSON(mod);
                        String monname = gerName.split(";")[1];
                        EmbedBuilder builder = new EmbedBuilder();
                        String sdname = getSDName(monname);
                        System.out.println("sdname = " + sdname);
                        JSONObject mon = json.getJSONObject(sdname);
                        System.out.println("mon = " + mon.toString(4));
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
                        List<JSONObject> list = getAllForms(monname, mod);
                        ArrayList<String> formeNames = list.stream().map(o -> toSDName(o.getString("name"))).collect(Collectors.toCollection(ArrayList::new));
                        //list.forEach(j -> System.out.println(j.toString(4)));
                        if (monname.equalsIgnoreCase("amigento") || monname.equalsIgnoreCase("arceus")) {
                            builder.addField("Typen", "Normal", false);
                        } else {
                            HashMap<String, ArrayList<String>> types = new HashMap<>();
                            for (JSONObject obj : list) {
                                System.out.println(obj);
                                String type = obj.getJSONArray("types").toList().stream().map(o -> {
                                    if (o.equals("Psychic")) return "Psycho";
                                    return getGerNameNoCheck((String) o);
                                }).collect(Collectors.joining(" "));

                                if (types.containsKey(type))
                                    types.get(type).add(obj.has("forme") ? obj.getString("forme") : "Normal");
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
                            if (heights.containsKey(d))
                                heights.get(d).add(obj.has("forme") ? obj.getString("forme") : "Normal");
                            else
                                heights.put(d, new ArrayList<>(Collections.singletonList(obj.has("forme") ? obj.getString("forme") : "Normal")));
                        }
                        StringBuilder height = new StringBuilder();
                        if (heights.size() == 1) {
                            height.append(heights.keySet().toArray(new Double[0])[0]).append(" m");
                        } else {
                            for (Double d : heights.keySet().stream().sorted(/*(o1, o2) -> {
                            ArrayList<String> l1 = heights.get(o1);
                            ArrayList<String> l2 = heights.get(o2);
                            if (l1.contains("Normal")) return -1;
                            if (l2.contains("Normal")) return 1;
                            return l1.get(0).compareTo(l2.get(0));
                        }*/).collect(Collectors.toList())) {
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
                            if (weights.containsKey(d))
                                weights.get(d).add(obj.has("forme") ? obj.getString("forme") : "Normal");
                            else
                                weights.put(d, new ArrayList<>(Collections.singletonList(obj.has("forme") ? obj.getString("forme") : "Normal")));
                        }
                        StringBuilder weight = new StringBuilder();
                        if (weights.size() == 1) {
                            weight.append(weights.keySet().toArray(new Double[0])[0]).append(" kg");
                        } else {
                            for (Double d : weights.keySet().stream().sorted(/*(o1, o2) -> {
                            ArrayList<String> l1 = weights.get(o1);
                            ArrayList<String> l2 = weights.get(o2);
                            if (l1.contains("Normal")) return -1;
                            if (l2.contains("Normal")) return 1;
                            return l1.get(0).compareTo(l2.get(0));
                        }*/).collect(Collectors.toList())) {
                                ArrayList<String> l = weights.get(d);
                                if (l.size() == 1 && l.contains("Normal")) weight.append(d).append(" kg\n");
                                else {
                                    weight.append(d).append(" kg (").append(String.join(", ", l)).append(")\n");
                                }
                            }
                        }
                        builder.addField("Gewicht", weight.toString(), true);
                        builder.addField("Eigruppe", mon.getJSONArray("eggGroups").toList().stream().map(o -> getGerNameNoCheck("E_" + o)).collect(Collectors.joining(", ")), true);
                        String baseforme = mon.has("baseForme") ? mon.getString("baseForme") : "Normal";
                        if (monname.equalsIgnoreCase("amigento") || monname.equalsIgnoreCase("arceus")) {
                            builder.addField("Fähigkeiten", monname.equalsIgnoreCase("amigento") ? "Alpha-System" : "Variabilität", false);
                        } else {
                            HashMap<String, ArrayList<String>> abis = new HashMap<>();
                            for (JSONObject obj : list) {
                                JSONObject o = obj.getJSONObject("abilities");
                                if (o.has("0")) {
                                    String a = getGerNameNoCheck(o.getString("0"));
                                    if (!abis.containsKey(a))
                                        abis.put(a, new ArrayList<>(Collections.singletonList(obj.has("forme") ? obj.getString("forme") : baseforme)));
                                    else abis.get(a).add(obj.has("forme") ? obj.getString("forme") : baseforme);
                                }
                                if (o.has("1")) {
                                    String a = getGerNameNoCheck(o.getString("1"));
                                    if (!abis.containsKey(a))
                                        abis.put(a, new ArrayList<>(Collections.singletonList(obj.has("forme") ? obj.getString("forme") : baseforme)));
                                    else abis.get(a).add(obj.has("forme") ? obj.getString("forme") : baseforme);
                                }
                                if (o.has("H")) {
                                    String a = getGerNameNoCheck(o.getString("H"));
                                    if (!abis.containsKey(a))
                                        abis.put(a, new ArrayList<>(Collections.singletonList((obj.has("forme") ? obj.getString("forme") : baseforme) + " VF")));
                                    else
                                        abis.get(a).add((obj.has("forme") ? obj.getString("forme") : baseforme) + " VF");
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
                            if (!str.contains("Alola VF") && !str.contains("Galar VF"))
                                str = str.replace("Normal VF", "VF");
                            builder.addField("Fähigkeiten", str, false);
                        }
                        if (monname.equalsIgnoreCase("amigento") || monname.equalsIgnoreCase("arceus")) {
                            builder.addField(monname.equalsIgnoreCase("amigento") ? "Amigento" : "Arceus", monname.equalsIgnoreCase("amigento") ? "KP: 95\n" + "Atk: 95\n" + "Def: 95\n" + "SpAtk: 95\n" + "SpDef: 95\n" + "Init: 95\n" + "Summe: 570"
                                    : "KP: 120\n" + "Atk: 120\n" + "Def: 120\n" + "SpAtk: 120\n" + "SpDef: 120\n" + "Init: 120\n" + "Summe: 720", false);
                        } else {
                            HashMap<String, ArrayList<String>> stat = new HashMap<>();
                            HashMap<String, String> origname = new HashMap<>();
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
                                StringBuilder toadd = new StringBuilder(obj.getString("name"));
                                ArrayList<String> split = new ArrayList<>(Arrays.asList(toadd.toString().split("-")));
                                if (toadd.toString().contains("-Alola")) {
                                    toadd = new StringBuilder("Alola-" + getGerNameNoCheck(split.get(0)));
                                    for (int i = 2; i < split.size(); i++) {
                                        toadd.append("-").append(split.get(i));
                                    }
                                } else if (toadd.toString().contains("-Galar")) {
                                    toadd = new StringBuilder("Galar-" + getGerNameNoCheck(split.get(0)));
                                    for (int i = 2; i < split.size(); i++) {
                                        toadd.append("-").append(split.get(i));
                                    }
                                } else if (toadd.toString().contains("-Mega")) {
                                    toadd = new StringBuilder("Mega-" + getGerNameNoCheck(split.get(0)));
                                    for (int i = 2; i < split.size(); i++) {
                                        toadd.append("-").append(split.get(i));
                                    }
                                } else if (split.size() > 1) {
                                    toadd = new StringBuilder(getGerNameNoCheck(split.remove(0)) + "-" + String.join("-", split));
                                } else toadd = new StringBuilder(getGerNameNoCheck(toadd.toString()));
                                origname.put(toadd.toString(), toSDName(obj.getString("name")));
                                if (stat.containsKey(str)) stat.get(str).add(toadd.toString());
                                else stat.put(str, new ArrayList<>(Collections.singletonList(toadd.toString())));
                            }
                            for (String s : stat.keySet().stream().sorted(Comparator.comparing(o -> stat.get(o).stream().mapToInt(str -> formeNames.indexOf(origname.get(str))).min().orElse(0))).collect(Collectors.toList())) {
                                builder.addField(String.join(", ", stat.get(s)), s, true);
                            }
                        }
                        if (msg.toLowerCase().contains("shiny") || msg.toLowerCase().contains("gummibärchen"))
                            builder.setImage(getShinySpriteJSON().getString(String.valueOf(mon.getInt("num"))));
                        else builder.setImage(getSpriteJSON().getString(String.valueOf(mon.getInt("num"))));
                        builder.setTitle(monname);
                        builder.setColor(Color.CYAN);
                        tco.sendMessage(builder.build()).queue();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    break;
                case "atk":
                    name = gerName.split(";")[1];
                    System.out.println(getMovesJSON(mod).toString(4));
                    JSONObject data = getMovesJSON(mod).getJSONObject(getSDName(name, mod));
                    String type = data.getString("type");
                    if (type.equals("Psychic")) type = "Psycho";
                    else type = getGerNameNoCheck(type);
                    String p;
                    int maxPower;
                    if (data.has("ohko")) {
                        p = "K.O.";
                        maxPower = 130;
                    } else {
                        int bp = data.getInt("basePower");
                        p = String.valueOf(bp);
                        if (Arrays.asList("Gift", "Kampf").contains(type)) {
                            if (bp >= 150) {
                                maxPower = 100;
                            } else if (bp >= 110) {
                                maxPower = 95;
                            } else if (bp >= 75) {
                                maxPower = 90;
                            } else if (bp >= 65) {
                                maxPower = 85;
                            } else if (bp >= 55) {
                                maxPower = 80;
                            } else if (bp >= 45) {
                                maxPower = 75;
                            } else {
                                maxPower = 70;
                            }
                        } else {
                            if (bp >= 150) {
                                maxPower = 150;
                            } else if (bp >= 110) {
                                maxPower = 140;
                            } else if (bp >= 75) {
                                maxPower = 130;
                            } else if (bp >= 65) {
                                maxPower = 120;
                            } else if (bp >= 55) {
                                maxPower = 110;
                            } else if (bp >= 45) {
                                maxPower = 100;
                            } else {
                                maxPower = 90;
                            }
                        }
                    }
                    String accuracy;
                    Object acc = data.get("accuracy");
                    if (acc instanceof Boolean) accuracy = "-";
                    else accuracy = acc + "%";
                    String cat = data.getString("category");
                    String category;
                    switch (cat) {
                        case "Physical":
                            category = "Physisch";
                            break;
                        case "Special":
                            category = "Speziell";
                            break;
                        case "Status":
                            category = "Status";
                            break;
                        default:
                            category = "ERROR";
                            break;
                    }
                    int ppc = data.getInt("pp");
                    String pp = ppc + " (max. " + (ppc * 8 / 5) + ")";
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setTitle(name)
                            .addField("English", getEnglName(name, mod), true)
                            .addField("Power", p, true)
                            .addField("Dyna-Power", String.valueOf(maxPower), true)
                            .addField("Accuracy", accuracy, true)
                            .addField("Category", category, true)
                            .addField("AP", pp, true)
                            .addField("Type", type, true)
                            .addField("Priority", String.valueOf(data.getInt("priority")), true)
                            .setColor(Color.CYAN)
                            .setDescription(getWikiJSON().getJSONObject("atkdata").getString(toSDName(getGerNameNoCheck(name))));
                    if (data.getString("category").equals("Status")) {
                        String text;
                        JSONObject eff = data.getJSONObject("zMove");
                        if (eff.has("effect")) {
                            switch (eff.getString("effect")) {
                                case "clearnegativeboost":
                                    text = "Negative Statusveränderungen werden zurückgesetzt";
                                    break;
                                case "crit2":
                                    text = "Critchance +2";
                                    break;
                                case "heal":
                                    text = "Volle Heilung";
                                    break;
                                case "curse":
                                    text = "Volle Heilung beim Geist Typ, sonst Atk +1";
                                    break;
                                case "redirect":
                                    text = "Spotlight";
                                    break;
                                case "healreplacement":
                                    text = "Heilt eingewechseltes Mon voll";
                                    break;
                                default:
                                    text = "Error";
                                    System.out.println(eff.toString(4));
                                    break;
                            }
                        } else {
                            JSONObject boosts = eff.getJSONObject("boost");
                            String stat = boosts.keys().next();
                            switch (stat) {
                                case "atk":
                                    text = "Atk +" + boosts.getInt(stat);
                                    break;
                                case "def":
                                    text = "Def +" + boosts.getInt(stat);
                                    break;
                                case "spa":
                                    text = "SpAtk +" + boosts.getInt(stat);
                                    break;
                                case "spd":
                                    text = "SpDef +" + boosts.getInt(stat);
                                    break;
                                case "spe":
                                    text = "Init +" + boosts.getInt(stat);
                                    break;
                                case "accuracy":
                                    text = "Genauigkeit +" + boosts.getInt(stat);
                                    break;
                                case "evasion":
                                    text = "Ausweichwert +" + boosts.getInt(stat);
                                    break;
                                default:
                                    text = "Error";
                            }
                        }
                        builder.addField("Z-Effect", text, true);
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
                    break;
                case "abi":
                    String abiname = gerName.split(";")[1];
                    tco.sendMessage(new EmbedBuilder().setTitle(abiname).setDescription("Englisch: " + getEnglName(abiname) + "\n" + getWikiJSON().getJSONObject("abidata").getString(toSDName(abiname))).setColor(Color.CYAN).build()).queue();
                    break;
                case "item":
                    String itemname = gerName.split(";")[1];
                    tco.sendMessage(new EmbedBuilder().setTitle(itemname).setDescription("Englisch: " + getEnglName(itemname) + "\n" + getWikiJSON().getJSONObject("itemdata").getString(toSDName(itemname))).setColor(Color.CYAN).build()).queue();
                    break;
                default:
                    tco.sendMessage("Es gibt kein(e) Pokemon/Attacke/Fähigkeit/Item mit dem Namen " + name + "!").queue();
                    break;
            }
            /*Document d;
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
            }*/
        } catch (Exception ex) {
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
            ex.printStackTrace();
        }
    }
}
