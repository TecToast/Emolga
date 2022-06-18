package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.selectmenus.selectmenusaves.SmogonSet;
import de.tectoast.jsolf.JSONArray;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Map;

public class SmogonCommand extends Command {
    public static final Map<String, String> statnames = Map.of("hp", "HP", "atk", "Atk", "def", "Def", "spa", "SpA", "spd", "SpD", "spe", "Spe");

    public SmogonCommand() {
        super("smogon", "Zeigt die vorgeschlagenen Smogon-Sets für Gen 8", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("form", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                        SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
                ), true)
                .addEngl("mon", "Pokemon", "Das Pokemon... lol", Translation.Type.POKEMON)
                .setExample("!smogon Primarene")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) throws IOException {
        TextChannel tco = e.getChannel();
        ArgumentManager args = e.getArguments();
        String name = args.getTranslation("mon").getTranslation();
        String form = args.has("form") ? "-" + args.getText("form").toLowerCase() : "";
        Document d = Jsoup.connect("https://www.smogon.com/dex/ss/pokemon/" + name.toLowerCase() + form + "/").get();
        JSONObject obj = new JSONObject(d.select("script").get(1).data().trim().substring("dexSettings = ".length())).getJSONArray("injectRpcs").getJSONArray(2).getJSONObject(1);
        if (obj.getJSONArray("strategies").length() == 0) {
            try {
                d = Jsoup.connect("https://www.smogon.com/dex/sm/pokemon/" + name.toLowerCase() + form + "/").get();
                obj = new JSONObject(d.select("script").get(1).data().trim().substring("dexSettings = ".length())).getJSONArray("injectRpcs").getJSONArray(2).getJSONObject(1);
                tco.sendMessage("Gen 7:").queue();
            } catch (Exception ex) {
                tco.sendMessage("Es gibt kein aktuelles Moveset für dieses Pokemon!").queue();
                return;
            }
        }
        JSONArray arr = obj.getJSONArray("strategies");
        SmogonSet smogon = new SmogonSet(arr);
        //noinspection ResultOfMethodCallIgnored
        e.reply(smogon.buildMessage(), ma -> ma.setActionRows(smogon.buildActionRows()), null, mes -> Command.smogonMenu.put(mes.getIdLong(), smogon), null);
    }
}

/*
for (int x = 0; ; x++) {
            try {
                JSONObject ms = json.getJSONArray("injectRpcs").getJSONArray(2).getJSONObject(1).getJSONArray("strategies").getJSONObject(0).getJSONArray("movesets").getJSONObject(x);
                StringBuilder abilities = new StringBuilder();
                for (int i = 0; ; i++) {
                    try {
                        abilities.append(ms.getJSONArray("abilities").getString(i)).append(" / ");
                    } catch (Exception ex) {
                        abilities = new StringBuilder(abilities.substring(0, abilities.length() - 3));
                        break;
                    }
                }
                JSONArray moveslots = ms.getJSONArray("moveslots");
                StringBuilder moves = new StringBuilder("- ");
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; ; j++) {
                        try {
                            moves.append(moveslots.getJSONArray(i).getJSONObject(j).getString("move")).append(" / ");
                        } catch (Exception ex) {
                            moves = new StringBuilder(moves.substring(0, moves.length() - 3));
                            moves.append("\n- ");
                            break;
                        }
                    }
                }
                moves = new StringBuilder(moves.substring(0, moves.length() - 3));
                StringBuilder nature = new StringBuilder();
                for (int i = 0; ; i++) {
                    try {
                        nature.append(ms.getJSONArray("natures").getString(i)).append(" / ");
                    } catch (Exception ex) {
                        nature = new StringBuilder(nature.substring(0, nature.length() - 3));
                        break;
                    }
                }
                StringBuilder items = new StringBuilder();
                for (int i = 0; ; i++) {
                    try {
                        items.append(ms.getJSONArray("items").getString(i)).append(" / ");
                    } catch (Exception ex) {
                        items = new StringBuilder(items.substring(0, items.length() - 3));
                        break;
                    }
                }
                JSONObject evsplit = ms.getJSONArray("evconfigs").getJSONObject(0);
                StringBuilder evs = new StringBuilder();
                if (evsplit.getInt("hp") > 0) evs.append(evsplit.getInt("hp")).append(" HP / ");
                if (evsplit.getInt("atk") > 0) evs.append(evsplit.getInt("atk")).append(" Atk / ");
                if (evsplit.getInt("def") > 0) evs.append(evsplit.getInt("def")).append(" Def / ");
                if (evsplit.getInt("spa") > 0) evs.append(evsplit.getInt("spa")).append(" SpA / ");
                if (evsplit.getInt("spd") > 0) evs.append(evsplit.getInt("spd")).append(" SpD / ");
                if (evsplit.getInt("spe") > 0) evs.append(evsplit.getInt("spe")).append(" Spe / ");
                evs = new StringBuilder(evs.substring(0, evs.length() - 3));

    String moveset = eachWordUpperCase(name) + " @ " + items + "\n" +
            "Ability: " + abilities + "\n" +
            "EVs: " + evs + "\n" +
            nature + " Nature" + "\n" +
            moves;
                tco.sendMessage(moveset).queue();
                        } catch (Exception ex) {
                        break;
                        }
                        }
 */