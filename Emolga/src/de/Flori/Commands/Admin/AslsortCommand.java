package de.Flori.Commands.Admin;

import com.google.api.services.sheets.v4.model.*;
import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import de.Flori.utils.Google;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

import java.util.*;

public class AslsortCommand extends Command {
    @Override
    public void process(GuildMessageReceivedEvent e) {
        String msg = e.getMessage().getContentDisplay();
        TextChannel tco = e.getChannel();
        String str = msg.substring(9);
        JSONObject json = getEmolgaJSON();
        JSONObject drafts = json.getJSONObject("drafts");
        if (!drafts.has(firstUpperCase(str) + "-Conference")) {
            tco.sendMessage("Ungültige Liga!").queue();
            return;
        }
        JSONObject league = drafts.getJSONObject(firstUpperCase(str) + "-Conference");
        String sid = league.getJSONObject("doc").getString("sid");
        int i;
        List<List<Object>> formula;
        List<List<Object>> points;
        try {
            formula = Google.get(sid, "Tabelle!B3:I12", true, false);
            points = Google.get(sid, "Tabelle!C3:J12", false, false);
        } catch (IllegalArgumentException IllegalArgumentException) {
            IllegalArgumentException.printStackTrace();
            return;
        }
        List<List<Object>> orig = new ArrayList<>(points);
        points.sort((o1, o2) -> {
            if (Integer.parseInt((String) o1.get(1)) != Integer.parseInt((String) o2.get(1))) {
                return Integer.compare(Integer.parseInt((String) o1.get(1)), Integer.parseInt((String) o2.get(1)));
            }
            if (Integer.parseInt((String) o1.get(7)) != Integer.parseInt((String) o2.get(7))) {
                return Integer.compare(Integer.parseInt((String) o1.get(7)), Integer.parseInt((String) o2.get(7)));
            }
            if (Integer.parseInt((String) o1.get(5)) != Integer.parseInt((String) o2.get(5))) {
                return Integer.compare(Integer.parseInt((String) o1.get(5)), Integer.parseInt((String) o2.get(5)));
            }
            //System.out.println(o1.get(0) + " oberhalb von " + o2.get(0) + "?");
            if (league.has("results")) return -1;
            JSONObject results = league.getJSONObject("results");
            String n1 = json.getJSONObject("docnames").getString((String) o1.get(0));
            String n2 = json.getJSONObject("docnames").getString((String) o2.get(0));
            if (results.has(n1 + ":" + n2)) {
                return results.getString(n1 + ":" + n2).equals(n1) ? 1 : -1;
            }
            if (results.has(n2 + ":" + n1)) {
                return results.getString(n2 + ":" + n1).equals(n1) ? 1 : -1;
            }
            return -1;
        });
        Collections.reverse(points);
        //System.out.println(points);
        Spreadsheet s;
        try {
            s = Google.getSheetData(sid, "Tabelle!B3:B12", false);
        } catch (IllegalArgumentException IllegalArgumentException) {
            IllegalArgumentException.printStackTrace();
            return;
        }
        Sheet sheet = s.getSheets().get(0);
        ArrayList<CellFormat> formats = new ArrayList<>();
        for (RowData rowDatum : sheet.getData().get(0).getRowData()) {
            formats.add(rowDatum.getValues().get(0).getEffectiveFormat());
        }
        HashMap<Integer, List<Object>> valmap = new HashMap<>();
        HashMap<Integer, CellFormat> formap = new HashMap<>();
        HashMap<Integer, List<Object>> namap = new HashMap<>();
        i = 0;
        for (List<Object> objects : orig) {
            List<Object> list = formula.get(i);
            Object logo = list.remove(0);
            Object name = list.remove(0);
            list.remove(0);
            list.remove(0);
            int index = points.indexOf(objects);
            valmap.put(index, list);
            formap.put(index, formats.get(i));
            namap.put(index, Arrays.asList(logo, name));
            i++;
        }
        List<List<Object>> senddata = new ArrayList<>();
        List<List<Object>> sendname = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            senddata.add(valmap.get(j));
            sendname.add(namap.get(j));
        }
        try {
            Google.updateRequest(sid, "Tabelle!F3", senddata, false, false);
            Google.updateRequest(sid, "Tabelle!B3", sendname, false, false);
        } catch (IllegalArgumentException IllegalArgumentException) {
            IllegalArgumentException.printStackTrace();
            return;
        }
        for (int j = 0; j < 10; j++) {
            Request request = new Request();
            request.setUpdateCells(new UpdateCellsRequest().setRows(Collections.singletonList(new RowData()
                    .setValues(getXTimes(new CellData()
                            .setUserEnteredFormat(new CellFormat()
                                    .setBackgroundColor(formap.get(j).getBackgroundColor()).setTextFormat(formap.get(j).getTextFormat().setFontSize(11).setFontFamily("Oswald"))), 9))))
                    .setFields("UserEnteredFormat(BackgroundColor,TextFormat)").setRange(new GridRange().setSheetId(553533374).setStartRowIndex(j + 2).setEndRowIndex(j + 3).setStartColumnIndex(1).setEndColumnIndex(10)));
            try {
                Google.batchUpdateRequest(sid, request, false);
            } catch (IllegalArgumentException IllegalArgumentException) {
                IllegalArgumentException.printStackTrace();
                return;
            }
        }
        tco.sendMessage("Done!").queue();
    }

    public AslsortCommand() {
        super("aslsort", "`!aslsort <Koko|Lele|Bulu|Fini>` Sortiert die Tabelle der Liga in der ASL", CommandCategory.Admin, "518008523653775366", "447357526997073930");
    }
}
