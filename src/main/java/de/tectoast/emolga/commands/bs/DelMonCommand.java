package de.tectoast.emolga.commands.bs;

import com.google.api.services.sheets.v4.model.*;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Google;
import de.tectoast.emolga.utils.RequestBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class DelMonCommand extends Command {
    public DelMonCommand() {
        super("delmon", "`!delmon <pokemon> <Ball>` Trägt ins Tauschdokument ein, dass du dieses pokemon in diesem Ball nicht mehr hast", CommandCategory.BS);
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay();
        Member member = e.getMember();
        String[] split = msg.split(" ");
        String pokemon = split[1];
        String pokeball = split[2];
        if (pokeball.equalsIgnoreCase("Pokeball")) pokeball = "Pokéball";
        Optional<String> opt = balls.stream().filter(pokeball::equalsIgnoreCase).findFirst();
        if (opt.isEmpty()) {
            tco.sendMessage("Dieser Ball existiert nicht!").queue();
            return;
        }
        String ball = opt.get();
        String mon;
        Optional<String> optmon = mons.stream().filter(pokemon::equalsIgnoreCase).findFirst();
        if (optmon.isPresent()) {
            mon = optmon.get();
        } else {
            Translation t;
            if (pokemon.toLowerCase().startsWith("a-")) t = getGerName(pokemon.substring(2)).before("A-");
            else if (pokemon.toLowerCase().startsWith("g-")) t = getGerName(pokemon.substring(2)).before("A-");
            else t = getGerName(pokemon);
            if (!t.isFromType(Translation.Type.POKEMON)) {
                tco.sendMessage("Dieses Pokemon existiert nicht!").queue();
                return;
            }
            mon = t.getTranslation();
        }
        if ((mon.equals("Hopplo") || mon.equals("Memmeon") || mon.equals("Chimpep")) && !ball.equals("Pokéball")) {
            tco.sendMessage("Die Starter können nur in einem normalen Pokéball sein!").queue();
            return;
        }
        if (!mons.contains(mon)) {
            tco.sendMessage("Dieses pokemon steht nicht im Tauschdokument!").queue();
            return;
        }
        JSONObject json = getEmolgaJSON();
        if (!json.has("tradedoc")) {
            tco.sendMessage("Du musst dich erst mit `!add` ins Doc eintragen!").queue();
            return;
        }
        JSONObject obj = json.getJSONObject("tradedoc");
        String id = null;
        for (String s : obj.keySet()) {
            if (obj.getString(s).equals(member.getId())) id = s;
        }
        if (id == null) {
            tco.sendMessage("Du musst dich erst mit `!add` ins Doc eintragen!").queue();
            return;
        }
        String range = "VFs und Ballmons!" + (ball.equals("Ultraball") ? "AA" : (char) (balls.indexOf(ball) + 68)) + (mons.indexOf(mon) + 3);
        List<List<Object>> list = Google.get(tradesid, range, false, false);
        ArrayList<Integer> l = list == null ? new ArrayList<>() : Arrays.stream(((String) list.get(0).get(0)).split(";")).map(Integer::parseInt).sorted().collect(Collectors.toCollection(ArrayList::new));
        if (!l.remove((Integer) Integer.parseInt(id))) {
            tco.sendMessage("Du besitzt im Tauschdokument kein " + mon + " in einem " + ball + "!").queue();
            return;
        }
        RequestBuilder b = new RequestBuilder(tradesid);
        b.addSingle(range, l.stream().sorted().map(String::valueOf).collect(Collectors.joining(";")));
        tco.sendMessage("Du hast eingetragen, dass du ein " + mon + " in einem " + ball + " nicht mehr hast!").queue();
        if (Google.get(tradesid, "VFs und Ballmons!D" + (mons.indexOf(mon) + 3) + ":AA" + (mons.indexOf(mon) + 3), false, false) == null) {
            Request req = new Request();
            req.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                    .setValues(Collections.singletonList(new CellData()
                            .setUserEnteredFormat(new CellFormat()
                                    .setBackgroundColor(new Color()
                                            .setRed((float) 1)))))))
                    .setFields("userEnteredFormat.backgroundColor").setRange(new GridRange().setSheetId(248731694).setStartRowIndex(mons.indexOf(mon) + 2).setEndRowIndex(mons.indexOf(mon) + 3).setStartColumnIndex(0).setEndColumnIndex(1)));
            b.addBatch(req);
        }
        b.execute();
    }
}
