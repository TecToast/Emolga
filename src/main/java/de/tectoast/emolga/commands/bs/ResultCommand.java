package de.tectoast.emolga.commands.bs;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Result;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import org.jsolf.JSONObject;

import java.util.*;
import java.util.function.Consumer;


public class ResultCommand extends Command {
    final HashSet<String> current = new HashSet<>();
    final ArrayList<String> CANCEL_WORDS = new ArrayList<>(Collections.singletonList("cancel"));

    public ResultCommand() {
        super("result", "`!result` Startet die interaktive Ergebniseingabe", CommandCategory.BS);
    }

    @Override
    public void process(GuildCommandEvent e) {
        if (current.contains(e.getAuthor().getId())) {
            e.getChannel().sendMessage("Du befindest dich bereits in einer Ergebnisaufnahme!").queue();
            return;
        }
        JSONObject bst = getEmolgaJSON().getJSONObject("BST");
        ArrayList<String> gdl = new ArrayList<>(Arrays.asList(bst.getJSONObject("battleorder").getString(String.valueOf(bst.getInt("gameday"))).split(";")));
        Member member = e.getMember();
        String p1 = member.getId();
        String p2;
        if (!checkBST) {
            Optional<String> op = gdl.stream().filter(str -> str.contains(p1)).findFirst();
            if (op.isEmpty()) {
                sendToUser(member, "Du spielst nicht in dieser Liga mit!");
                return;
            }
            String[] bosplit = op.get().split(":");
            p2 = bosplit[0].equals(p1) ? bosplit[1] : bosplit[0];
        } else p2 = "728202578353193010";
        int gameday = bst.getInt("gameday");
        if (bst.has("results") && !checkBST) {
            JSONObject res = bst.getJSONObject("results");
            if (res.has(String.valueOf(gameday))) {
                JSONObject obj = res.getJSONObject(String.valueOf(gameday));
                if (obj.has(p1 + ";" + p2) || obj.has(p2 + ";" + p1)) {
                    e.getChannel().sendMessage("Der Kampf zwischen **" + e.getGuild().retrieveMemberById(p1).complete().getEffectiveName() + "** und **" + e.getGuild().retrieveMemberById(p2).complete().getEffectiveName() + "** an Spieltag " + gameday + " ist bereits eingetragen!").queue();
                    return;
                }
            }
        }
        sendToUser(Long.parseLong(p1), "Wieviele Spiele habt ihr gespielt?");
        e.getChannel().sendMessage("Ich habe dir privat geschrieben!").queue();
        Result r = new Result(p1, p2);
        r.gameday = gameday;
        waitForGamesCount(e.getEvent(), r);
    }

    private void waitForGamesCount(GuildMessageReceivedEvent event, Result r) {
        wait(event.getAuthor(), e -> {
            String msg = e.getMessage().getContentDisplay();
            int gamescount;
            try {
                gamescount = Integer.parseInt(msg);
            } catch (NumberFormatException numberFormatException) {
                sendToUser(e.getAuthor(), "Das ist keine valide Zahl!");
                waitForGamesCount(event, r);
                return;
            }
            if (gamescount < 2 || gamescount > 3) {
                sendToUser(e.getAuthor(), "Du musst entweder 2 oder 3 schreiben!");
                waitForGamesCount(event, r);
                return;
            }
            r.gamecount = gamescount;
            sendToUser(e.getAuthor(), "Welches war das **" + r.currmoncount + ".** pokemon von **" + r.name1 + "**?");
            waitForMon(e.getAuthor(), r);
        });
    }

    private void waitForMon(User u, Result r) {
        wait(u, e -> {
            String msg = e.getMessage().getContentDisplay();
            Translation t = getBSTGerName(msg);
            if (t.getTranslation().equals("ONLYWITHFORM")) {
                sendToUser(u, "Du musst bei Wulaosu -Unlicht oder -Wasser hinterschreiben!");
                waitForMon(u, r);
                return;
            }
            if (!t.isFromType(Translation.Type.POKEMON)) {
                sendToUser(u, "Das ist kein pokemon!");
                waitForMon(u, r);
                return;
            }
            String mon = t.getTranslation();
            r.currmon = mon;
            if (r.curruser.equals(r.u1)) r.mons1.add(mon);
            else r.mons2.add(mon);
            r.currmoncount++;
            sendToUser(u, "War **" + mon + "** im **" + r.currgamecount + ".** Spiel dabei? (j/n)");
            waitForPresent(u, r);
        });
    }

    private void waitForPresent(User u, Result r) {
        wait(u, e -> {
            String msg = e.getMessage().getContentDisplay();
            if (msg.equalsIgnoreCase("j")) {
                if (r.curruser.equals(r.u1)) {
                    if (!r.uses1.containsKey(r.currmon)) r.uses1.put(r.currmon, new ArrayList<>());
                    r.uses1.get(r.currmon).add(r.currgamecount);
                } else {
                    if (!r.uses2.containsKey(r.currmon)) r.uses2.put(r.currmon, new ArrayList<>());
                    r.uses2.get(r.currmon).add(r.currgamecount);
                }
                sendToUser(u, "Okay, wieviele Kills hat **" + r.currmon + "** von **" + r.currname + "** im **" + r.currgamecount + ".** Kampf gemacht?");
                waitForKills(u, r);
            } else if (msg.equalsIgnoreCase("n")) {
                r.currgamecount++;
                if (r.currgamecount > r.gamecount) {
                    r.currgamecount = 1;
                    if (r.currmoncount > 6) {
                        if (r.curruser.equals(r.u2)) {
                            end(u, r);
                            return;
                        }
                        r.currmoncount = 1;
                        r.curruser = r.u2;
                        r.currname = r.name2;
                        sendToUser(e.getAuthor(), "Welches war das **" + r.currmoncount + ".** pokemon von **" + r.currname + "**?");
                        waitForMon(u, r);
                    } else {
                        sendToUser(e.getAuthor(), "Welches war das **" + r.currmoncount + ".** pokemon von **" + r.currname + "**?");
                        waitForMon(e.getAuthor(), r);
                    }
                } else {
                    sendToUser(u, "War **" + r.currmon + "** im **" + r.currgamecount + ".** Spiel dabei? (j/n)");
                    waitForPresent(u, r);
                }
            } else {
                sendToUser(u, "Das ist kein j oder n!");
                waitForPresent(u, r);
            }
        });
    }

    private void waitForKills(User u, Result r) {
        wait(u, e -> {
            String msg = e.getMessage().getContentDisplay();
            int i;
            try {
                i = Integer.parseInt(msg);
            } catch (NumberFormatException numberFormatException) {
                sendToUser(u, "Das ist keine Zahl!");
                waitForKills(u, r);
                return;
            }
            if (i < 0 || i > 4) {
                sendToUser(u, "Die Zahl darf nicht kleiner als 0 oder größer als 4 sein!");
                waitForKills(u, r);
                return;
            }
            if (r.curruser.equals(r.u1)) {
                if (!r.kills1.containsKey(r.currmon)) {
                    r.kills1.put(r.currmon, new HashMap<>());
                }
                if (!r.deaths1.containsKey(r.currmon)) {
                    r.deaths1.put(r.currmon, new ArrayList<>());
                }
                r.kills1.get(r.currmon).put(r.currgamecount, i);
            } else {
                if (!r.kills2.containsKey(r.currmon)) {
                    r.kills2.put(r.currmon, new HashMap<>());
                }
                if (!r.deaths2.containsKey(r.currmon)) {
                    r.deaths2.put(r.currmon, new ArrayList<>());
                }
                r.kills2.get(r.currmon).put(r.currgamecount, i);
            }
            sendToUser(u, "Ok, ist **" + r.currmon + "** im **" + r.currgamecount + ".** Kampf gestorben? (a/d)");
            waitForDeath(u, r);
        });
    }

    private void waitForDeath(User u, Result r) {
        wait(u, e -> {
            String msg = e.getMessage().getContentDisplay();
            if (msg.equalsIgnoreCase("a") || msg.equalsIgnoreCase("d")) {
                if (msg.equalsIgnoreCase("d")) {
                    if (r.curruser.equals(r.u1)) {
                        r.deaths1.get(r.currmon).add(r.currgamecount);
                    } else {
                        r.deaths2.get(r.currmon).add(r.currgamecount);
                    }
                }
                r.currgamecount++;
                if (r.currgamecount > r.gamecount) {
                    r.currgamecount = 1;
                    if (r.currmoncount > 6) {
                        if (r.curruser.equals(r.u2)) {
                            end(u, r);
                            return;
                        }
                        r.currmoncount = 1;
                        r.curruser = r.u2;
                        r.currname = r.name2;
                    }
                    sendToUser(e.getAuthor(), "Welches war das **" + r.currmoncount + ".** pokemon von **" + r.currname + "**?");
                    waitForMon(u, r);
                } else {
                    sendToUser(u, "War **" + r.currmon + "** im **" + r.currgamecount + ".** Spiel dabei? (j/n)");
                    waitForPresent(u, r);
                }
            } else {
                sendToUser(u, "Das ist kein a oder d!");
                waitForDeath(u, r);
            }
        });
    }

    private void end(User u, Result r) {
        current.remove(u.getId());
        r.calculate();
        sendToUser(u, r.buildMessage());
        sendToUser(u, "Überprüfe dieses Ergebnis und wenn es richtig ist, dann schick es in <#759712094223728650>!");
        //EmolgaMain.jda.getTextChannelById("759712094223728650").sendMessage(r.buildMessage()).queue();
        //r.doc();
    }

    private void wait(User u, Consumer<PrivateMessageReceivedEvent> action) {
        EmolgaMain.messageWaiter.waitForPrivateMessageReceived(
                e -> e.getAuthor().getId().equals(u.getId()),
                e ->
                {
                    if (CANCEL_WORDS.contains(e.getMessage().getContentRaw().toLowerCase())) {
                        sendToUser(u, "Die Ergebnisaufnahme wurde abgebrochen.");
                        current.remove(u.getId());
                        return;
                    }
                    action.accept(e);
                });
    }
}
