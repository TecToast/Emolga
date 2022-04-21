package de.tectoast.emolga.selectmenus;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.*;
import static de.tectoast.emolga.commands.pokemon.DataCommand.getPrevoInfo;

public class MonDataMenu extends MenuListener {

    private static final Logger logger = LoggerFactory.getLogger(MonDataMenu.class);

    public MonDataMenu() {
        super("mondata");
    }

    @Override
    public void process(SelectMenuInteractionEvent e) {
        /*e.reply("Dieses Menü funktioniert noch nicht, aber Flo arbeitet zurzeit daran :3").setEphemeral(true).queue();
        if (true) return;*/
        logger.info("e.getMessageIdLong() = " + e.getMessageIdLong());
        String name = e.getValues().get(0);
        /*MonData dt = monDataButtons.get(e.getMessageIdLong());
        if (dt == null) {
            e.editMessageEmbeds(new EmbedBuilder().setTitle("Ach Mensch " + e.getMember().getEffectiveName() + ", diese Mon-Data funktioniert nicht mehr, da seitdem der Bot neugestartet wurde!").setColor(Color.CYAN).build()).queue();
            return;
        }*/
        JSONObject mon = getDataJSON().getJSONObject(name);
        EmbedBuilder builder = new EmbedBuilder();
        builder.addField("Englisch", mon.getString("name"), true);
        builder.addField("Dex", String.valueOf(mon.getInt("num")), true);
        String gender;
        if (mon.has("genderRatio")) {
            JSONObject gen = mon.getJSONObject("genderRatio");
            gender = gen.getDouble("M") * 100 + "% ♂ " + gen.getDouble("F") * 100 + "% ♀";
        } else if (mon.has("gender")) {
            gender = mon.getString("gender").equals("M") ? "100% ♂" : (mon.getString("gender").equals("F") ? "100% ♀" : "Unbekannt");
        } else gender = "50% ♂ 50% ♀";
        builder.addField("Geschlecht", gender, true);
        //list.forEach(j -> logger.info(j.toString(4)));
        String monname = mon.getString("name");
        if (monname.equalsIgnoreCase("silvally") || monname.equalsIgnoreCase("arceus")) {
            builder.addField("Typen", "Normal", false);
        } else {
            HashMap<String, ArrayList<String>> types = new HashMap<>();

            logger.info(String.valueOf(mon));
            String type = mon.getJSONArray("types").toList().stream().map(o -> {
                if (o.equals("Psychic")) return "Psycho";
                return getGerNameNoCheck((String) o);
            }).collect(Collectors.joining(" "));

            builder.addField("Typen", type, false);
        }
        builder.addField("Größe", mon.getDouble("heightm") + " m", true);
        builder.addField("Gewicht", mon.getDouble("weightkg") + " kg", true);
        builder.addField("Eigruppe", mon.getJSONArray("eggGroups").toList().stream().map(o -> getGerNameNoCheck("E_" + o)).collect(Collectors.joining(", ")), true);
        String baseforme = mon.has("baseForme") ? mon.getString("baseForme") : "Normal";
        if (monname.equalsIgnoreCase("silvally") || monname.equalsIgnoreCase("arceus")) {
            builder.addField("Fähigkeiten", monname.equalsIgnoreCase("silvally") ? "Alpha-System" : "Variabilität", false);
        } else {
            HashMap<String, ArrayList<String>> abis = new HashMap<>();
            JSONObject o = mon.getJSONObject("abilities");
            StringBuilder b = new StringBuilder();
            if (o.has("0")) {
                b.append(getGerNameNoCheck(o.getString("0"))).append("\n");
            }
            if (o.has("1")) {
                b.append(getGerNameNoCheck(o.getString("1"))).append("\n");
            }
            if (o.has("H")) {
                b.append(getGerNameNoCheck(o.getString("H"))).append(" (VF)");
            }
            builder.addField("Fähigkeiten", b.toString(), false);
        }
                        /*if (mon.has("prevo")) {
                            if(list.size() == 1) {
                                builder.addField("Vorentwicklung", getPrevoInfo(mon), false);
                            } else {
                                for (JSONObject obj : list) {
                                    builder.addField(name + "-" + (obj.optString("baseForme", obj.optString("forme", ""))), getPrevoInfo(obj), true);
                                }
                            }
                        }*/
        if (monname.equalsIgnoreCase("silvally") || monname.equalsIgnoreCase("arceus")) {
            builder.addField(monname.equalsIgnoreCase("silvally") ? "Amigento" : "Arceus", monname.equalsIgnoreCase("silvally") ? """
                    KP: 95
                    Atk: 95
                    Def: 95
                    SpAtk: 95
                    SpDef: 95
                    Init: 95
                    Summe: 570"""
                    : """
                    KP: 120
                    Atk: 120
                    Def: 120
                    SpAtk: 120
                    SpDef: 120
                    Init: 120
                    Summe: 720""", false);
        } else {
            HashMap<String, ArrayList<String>> stat = new HashMap<>();
            HashMap<String, JSONObject> origname = new HashMap<>();

            JSONObject stats = mon.getJSONObject("baseStats");
            int kp = stats.getInt("hp");
            int atk = stats.getInt("atk");
            int def = stats.getInt("def");
            int spa = stats.getInt("spa");
            int spd = stats.getInt("spd");
            int spe = stats.getInt("spe");
            String str = "KP: " + kp + "\nAtk: " + atk + "\nDef: " + def + "\nSpAtk: " + spa
                    + "\nSpDef: " + spd + "\nInit: " + spe + "\nSumme: " + (kp + atk + def + spa + spd + spe);

                                /*origname.put(toadd.toString(), obj);
                                if (stat.containsKey(str)) stat.get(str).add(toadd.toString());
                                else stat.put(str, new ArrayList<>(Collections.singletonList(toadd.toString())));*/
            String prevoInfo = getPrevoInfo(mon);
            if (!prevoInfo.equals("")) {
                builder.addField("Erhaltbarkeit", prevoInfo, false);
            }
            builder.addField("Basestats", str, false);

                            /*for (String s : stat.keySet().stream().sorted(Comparator.comparing(o -> stat.get(o).stream().mapToInt(str -> formeNames.indexOf(toSDName(origname.get(str).getString("name")))).min().orElse(0))).collect(Collectors.toList())) {
                                builder.addField(String.join(", ", stat.get(s)), stat.get(s).stream().map(origname::get).map(DataCommand::getPrevoInfo).collect(Collectors.joining("")) + s, true);
                            }*/
        }
        builder.setImage(getGen5SpriteWithoutGoogle(mon));
        builder.setTitle(getGerNameWithForm(monname));
        builder.setColor(Color.CYAN);
        e.editMessageEmbeds(builder.build()).setActionRow(SelectMenu.create("mondata").addOptions(e.getSelectMenu().getOptions().stream().map(o -> o.withDefault(o.getValue().equals(name))).collect(Collectors.toList())).build()).queue();
        //e.getHook().editOriginalEmbeds(builder.build()).queue();
    }
}
