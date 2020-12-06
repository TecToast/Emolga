package de.Flori.Commands.Pokemon;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class SmogonCommand extends Command {
    public SmogonCommand() {
        super("smogon", "`!smogon <Pokemon>` Zeigt die vorgeschlagenen Smogon-Sets für Gen 8", CommandCategory.Pokemon);
    }


    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String name = msg.substring(8);
        Document wiki;
        Document d;
        try {
            d = Jsoup.connect("https://www.smogon.com/dex/ss/pokemon/" + name.toLowerCase() + "/").get();
        } catch (Exception ex) {
            try {
                wiki = Jsoup.connect("https://www.pokewiki.de/" + name).get();
                name = wiki.select("span").get(18).text();
                if (name.equals("en")) name = wiki.select("span").get(19).text();
                d = Jsoup.connect("https://www.smogon.com/dex/ss/pokemon/" + name.toLowerCase() + "/").get();
            } catch (Exception exception) {
                tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
                return;
            }
        }

        if (new JSONObject(d.select("script").first().html().substring(14)).getJSONArray("injectRpcs").getJSONArray(2).getJSONObject(1).getJSONArray("strategies").length() == 0) {
            try {
                d = Jsoup.connect("https://www.smogon.com/dex/sm/pokemon/" + name.toLowerCase() + "/").get();
                tco.sendMessage("Gen 7:").queue();
            } catch (Exception ex) {
                tco.sendMessage("Es gibt kein Moveset für dieses Pokemon!").queue();
                return;
            }
        }
        JSONObject json = new JSONObject(d.select("script").first().html().substring(14));
        tco.sendMessage("Format: " + json.getJSONArray("injectRpcs").getJSONArray(2).getJSONObject(1).getJSONArray("strategies").getJSONObject(0).getString("format")).queue();
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
                    for(int j = 0; ; j++) {
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
        /*System.out.println(moves);
        System.out.println(nature);
        System.out.println(abilities);
        System.out.println(items);
        System.out.println(evs);*/

                String moveset = eachWordUpperCase(name) + " @ " + items.toString() + "\n" +
                        "Ability: " + abilities.toString() + "\n" +
                        "EVs: " + evs.toString() + "\n" +
                        nature.toString() + " Nature" + "\n" +
                        moves.toString();
                tco.sendMessage(moveset).queue();
            } catch (Exception ex) {
                break;
            }
        }
    }
}
