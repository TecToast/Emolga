package de.tectoast.emolga.utils;

import de.tectoast.emolga.bot.EmolgaMain;

import java.util.ArrayList;
import java.util.HashMap;

public class Result {
    public final String u1;
    public final String u2;
    public final ArrayList<String> mons1 = new ArrayList<>();
    public final ArrayList<String> mons2 = new ArrayList<>();
    public final ArrayList<String> results = new ArrayList<>();
    public final HashMap<String, ArrayList<Integer>> uses1 = new HashMap<>();
    public final HashMap<String, ArrayList<Integer>> uses2 = new HashMap<>();
    public final HashMap<String, HashMap<Integer, Integer>> kills1 = new HashMap<>();
    public final HashMap<String, HashMap<Integer, Integer>> kills2 = new HashMap<>();
    public final HashMap<String, ArrayList<Integer>> deaths1 = new HashMap<>();
    public final HashMap<String, ArrayList<Integer>> deaths2 = new HashMap<>();
    public final String name1;
    public final String name2;
    public final ArrayList<Integer> wins1 = new ArrayList<>();
    public int gameday = -1;
    public int gamecount = -1;
    public int currgamecount = 1;
    public String curruser;
    public String currname;
    public String currmon = "";
    public int currmoncount = 1;
    //public int k1sum;
    //public int d1sum;

    public Result(String u1, String u2) {
        this.u1 = u1;
        this.u2 = u2;
        this.name1 = EmolgaMain.emolgajda.getGuildById("712035338846994502").retrieveMemberById(u1).complete().getEffectiveName();
        this.name2 = EmolgaMain.emolgajda.getGuildById("712035338846994502").retrieveMemberById(u2).complete().getEffectiveName();
        this.curruser = u1;
        this.currname = name1;
    }

    /*public Result(int gameday, int gamecount, int currgamecount, String curruser, String currname, String currmon, int currmoncount, String u1, String u2, ArrayList<String> mons1, ArrayList<String> mons2, ArrayList<String> results, HashMap<String, ArrayList<Integer>> uses1, HashMap<String, ArrayList<Integer>> uses2, HashMap<String, HashMap<Integer, Integer>> kills1, HashMap<String, HashMap<Integer, Integer>> kills2, HashMap<String, ArrayList<Integer>> deaths1, HashMap<String, ArrayList<Integer>> deaths2, String name1, String name2, ArrayList<Integer> wins1, int k1sum, int d1sum) {
        this.gameday = gameday;
        this.gamecount = gamecount;
        this.currgamecount = currgamecount;
        this.curruser = curruser;
        this.currname = currname;
        this.currmon = currmon;
        this.currmoncount = currmoncount;
        this.u1 = u1;
        this.u2 = u2;
        this.mons1 = new ArrayList<>(mons1);
        this.mons2 = new ArrayList<>(mons2);
        this.results = new ArrayList<>(results);
        this.uses1 = new HashMap<>(uses1);
        this.uses2 = new HashMap<>(uses2);
        this.kills1 = new HashMap<>(kills1);
        this.kills2 = new HashMap<>(kills2);
        this.deaths1 = new HashMap<>(deaths1);
        this.deaths2 = new HashMap<>(deaths2);
        this.name1 = name1;
        this.name2 = name2;
        this.wins1 = new ArrayList<>(wins1);
        this.k1sum = k1sum;
        this.d1sum = d1sum;
    }

    public Result createCopy() {
        return new Result(gameday, gamecount, currgamecount, curruser, currname, currmon, currmoncount, u1, u2, mons1, mons2, results, uses1, uses2, kills1, kills2, deaths1, deaths2, name1, name2, wins1, k1sum, d1sum);
    }*/

    public void calculate() {
        HashMap<Integer, Integer> kills = new HashMap<>();
        HashMap<Integer, Integer> deaths = new HashMap<>();
        for (String s : kills1.keySet()) {
            HashMap<Integer, Integer> kmap = kills1.get(s);
            for (Integer integer : kmap.keySet()) {
                kills.put(integer, kills.getOrDefault(integer, 0) + kmap.get(integer));
            }
            ArrayList<Integer> dlist = deaths1.get(s);
            for (Integer integer : dlist) {
                deaths.put(integer, deaths.getOrDefault(integer, 0) + 1);
            }
        }
        for (int i = 0; i < gamecount; i++) {
            int x = kills.getOrDefault(i + 1, 0) - deaths.getOrDefault(i + 1, 0);
            if (x > 0) {
                results.add(x + ":0");
                wins1.add(i + 1);
            } else results.add("0:" + Math.abs(x));
        }
    }

    public String buildMessage() {
        System.out.println("uses2 = " + uses2);
        System.out.println("kills2 = " + kills2);
        System.out.println("deaths2 = " + deaths2);
        StringBuilder str = new StringBuilder("Spieltag " + gameday + "\n<@" + u1 + "> vs <@" + u2 + ">\nSieger: ");
        str.append(wins1.size() == 2 ? name1 : name2).append("\n");
        str.append("Ergebnis: ").append(wins1.size()).append(":").append(results.size() - wins1.size()).append("\n\n");
        for (int i = 0; i < gamecount; i++) {
            str.append("Kampf ").append(i + 1).append(":\nSieger: ").append(wins1.contains(i + 1) ? name1 : name2).append("\nErgebnis: ").append(results.get(i)).append("\n\n").append(name1).append(":\n\n");
            for (String s : mons1) {
                str.append(s).append(" ");
                if (uses1.getOrDefault(s, new ArrayList<>()).contains(i + 1)) {
                    str.append(kills1.get(s).get(i + 1)).append(" ").append(deaths1.get(s).contains(i + 1) ? "D" : "A");
                } else str.append("/");
                str.append("\n");
            }
            str.append("\n").append(name2).append(":\n\n");
            for (String s : mons2) {
                str.append(s).append(" ");
                System.out.println("s = " + s);
                if (uses2.getOrDefault(s, new ArrayList<>()).contains(i + 1)) {
                    str.append(kills2.get(s).get(i + 1)).append(" ").append(deaths2.get(s).contains(i + 1) ? "D" : "A");
                } else str.append("/");
                str.append("\n");
            }
            str.append("\n");
        }
        return str.toString();
    }

    /*public void doc() {
        List<List<Object>> r1 = new ArrayList<>();
        List<List<Object>> r2 = new ArrayList<>();
        r1.add(Collections.singletonList(wins1.size()));
        r2.add(Collections.singletonList(results.size() - wins1.size()));

        for (String result : results) {
            String[] split = result.split(":");
            r1.add(Collections.singletonList(split[0]));
            r2.add(Collections.singletonList(split[1]));
        }
        JSONObject obj = getStatisticsJSON();
        ArrayList<String> statorder = new ArrayList<>();
        if (!obj.getString("order").equals(""))
            statorder.addAll(Arrays.asList(obj.getString("order").split(",")));
        ArrayList<String> origstatorder = new ArrayList<>(statorder);
        System.out.println("origstatorder1 = " + origstatorder);
        for (String s : mons1) {
            if (!obj.has(s)) {
                obj.put(s, new JSONObject());
                statorder.add(s);
            }
            JSONObject o = obj.getJSONObject(s);
            if (!o.has("usagerate")) o.put("usagerate", 0);
            if (!o.has("winrate")) o.put("winrate", 0);
            if (!o.has("kills")) o.put("kills", 0);
            if (!o.has("deaths")) o.put("deaths", 0);
            o.put("usagerate", o.getInt("usagerate") + (uses1.getOrDefault(s, new ArrayList<>()).size()));
            int wins = 0;
            for (Integer integer : uses1.getOrDefault(s, new ArrayList<>())) {
                if (wins1.contains(integer)) wins++;
            }
            o.put("winrate", o.getInt("winrate") + wins);
            o.put("kills", o.getInt("kills") + kills1.getOrDefault(s, new HashMap<>()).values().stream().mapToInt(i -> i).sum());
            o.put("deaths", o.getInt("deaths") + deaths1.getOrDefault(s, new ArrayList<>()).size());
        }
        for (String s : mons2) {
            if (!obj.has(s)) {
                obj.put(s, new JSONObject());
                statorder.add(s);
            }
            JSONObject o = obj.getJSONObject(s);
            if (!o.has("usagerate")) o.put("usagerate", 0);
            if (!o.has("winrate")) o.put("winrate", 0);
            if (!o.has("kills")) o.put("kills", 0);
            if (!o.has("deaths")) o.put("deaths", 0);
            o.put("usagerate", o.getInt("usagerate") + (uses2.getOrDefault(s, new ArrayList<>()).size()));
            int wins = 0;
            for (Integer integer : uses2.getOrDefault(s, new ArrayList<>())) {
                if (!wins1.contains(integer)) wins++;
            }
            o.put("winrate", o.getInt("winrate") + wins);
            o.put("kills", o.getInt("kills") + kills2.getOrDefault(s, new HashMap<>()).values().stream().mapToInt(i -> i).sum());
            o.put("deaths", o.getInt("deaths") + deaths2.getOrDefault(s, new ArrayList<>()).size());
        }
        System.out.println("origstatorder2 = " + origstatorder);
        obj.put("order", String.join(",", statorder));
        obj.put("games", obj.getInt("games") + results.size());
        saveStatisticsJSON();
        //System.out.println("k1 = " + k1);
        //System.out.println("k2 = " + k2);
        JSONObject bst = getEmolgaJSON().getJSONObject("BST");
        ArrayList<String> gdl = new ArrayList<>(Arrays.asList(bst.getJSONObject("battleorder").getString(String.valueOf(gameday)).split(";")));
        String sid = bst.getString("sid");
        ArrayList<String> tab = new ArrayList<>(Arrays.asList(bst.getString("table").split(",")));
        int index1 = tab.indexOf(u1);
        int index2 = tab.indexOf(u2);
        int x1 = index1;
        int y1 = 0;
        while (index1 - 4 * y1 > 3) {
            x1 -= 4;
            y1++;
        }
        int x2 = index2;
        int y2 = 0;
        while (index2 - 4 * y2 > 3) {
            x2 -= 4;
            y2++;
        }
        x1 = x1 * 10 + 3;
        y1 = y1 * 17 + 3;
        x2 = x2 * 10 + 3;
        y2 = y2 * 17 + 3;
        if (y1 == 71) x1 += 10;
        if (y2 == 71) x2 += 10;
        //List<Object> gwins1 = Google.get(sid, "Teilnehmer!" + getAsXCoord(x1) + y1 + ":" + getAsXCoord(x1 + 1) + y1, false, false).get(0);
        //List<Object> gwins2 = Google.get(sid, "Teilnehmer!" + getAsXCoord(x2) + y2 + ":" + getAsXCoord(y1 + 1) + y2, false, false).get(0);
        if (!bst.has("playerstats")) bst.put("playerstats", new JSONObject());
        JSONObject playerstats = bst.getJSONObject("playerstats");
        if (!playerstats.has(u1)) playerstats.put(u1, new JSONObject());
        if (!playerstats.has(u2)) playerstats.put(u2, new JSONObject());
        JSONObject stat1 = playerstats.getJSONObject(u1);
        JSONObject stat2 = playerstats.getJSONObject(u2);
        if (!stat1.has("wins")) stat1.put("wins", 0);
        if (!stat2.has("wins")) stat2.put("wins", 0);
        if (!stat1.has("looses")) stat1.put("looses", 0);
        if (!stat2.has("looses")) stat2.put("looses", 0);
        if (!stat1.has("kills")) stat1.put("kills", 0);
        if (!stat2.has("kills")) stat2.put("kills", 0);
        if (!stat1.has("deaths")) stat1.put("deaths", 0);
        if (!stat2.has("deaths")) stat2.put("deaths", 0);
        if (wins1.size() == 2) {
            stat1.put("wins", stat1.getInt("wins") + 1);
            stat2.put("looses", stat2.getInt("looses") + 1);
        } else {
            stat2.put("wins", stat2.getInt("wins") + 1);
            stat1.put("looses", stat1.getInt("looses") + 1);
        }
        Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x1) + y1, new ValueRange().setValues(Collections.singletonList(Arrays.asList(stat1.getInt("wins"), stat1.getInt("looses")))), false, false);
        Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x2) + y2, new ValueRange().setValues(Collections.singletonList(Arrays.asList(stat2.getInt("wins"), stat2.getInt("looses")))), false, false);
        stat1.put("kills", stat1.getInt("kills") + k1sum);
        stat2.put("kills", stat2.getInt("kills") + d1sum);
        stat1.put("deaths", stat1.getInt("deaths") + d1sum);
        stat2.put("deaths", stat2.getInt("deaths") + k1sum);
        if (!bst.has("results")) bst.put("results", new JSONObject());
        JSONObject resultsj = bst.getJSONObject("results");
        if (!resultsj.has(String.valueOf(gameday))) resultsj.put(String.valueOf(gameday), new JSONObject());
        JSONObject resgd = resultsj.getJSONObject(String.valueOf(gameday));
        resgd.put(u1 + ";" + u2, wins1.size() == 2 ? u1 : u2);
        saveEmolgaJSON();
        Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x1 + 5) + (y1 + gameday + 3), new ValueRange().setValues(Collections.singletonList(Arrays.asList(k1sum, d1sum))), false, false);
        Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x2 + 5) + (y2 + gameday + 3), new ValueRange().setValues(Collections.singletonList(Arrays.asList(d1sum, k1sum))), false, false);
        int ip = gdl.indexOf(gdl.stream().filter(s -> s.contains(u1)).collect(Collectors.joining("")));
        if (r1.size() == 3) r1.add(Collections.emptyList());
        if (r2.size() == 3) r2.add(Collections.emptyList());
        r1.add(Collections.emptyList());
        r1.add(Collections.singletonList(k1sum));
        r1.add(Collections.singletonList(d1sum));
        r2.add(Collections.emptyList());
        r2.add(Collections.singletonList(d1sum));
        r2.add(Collections.singletonList(k1sum));
        if (gdl.get(ip).split(":")[0].equals(u1)) {
            Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5 - 4) + (ip * 8 + 7), new ValueRange().setValues(Collections.singletonList(Collections.singletonList(stat1.getInt("wins") + "-" + stat1.getInt("looses")))), false, false);
            Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5) + (ip * 8 + 7), new ValueRange().setValues(Collections.singletonList(Collections.singletonList(stat2.getInt("wins") + "-" + stat2.getInt("looses")))), false, false);
            Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5 - 3) + (ip * 8 + 3), new ValueRange().setValues(r1), false, false);
            Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5 - 1) + (ip * 8 + 3), new ValueRange().setValues(r2), false, false);
        } else {
            Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5 - 4) + (ip * 8 + 7), new ValueRange().setValues(Collections.singletonList(Collections.singletonList(stat2.getInt("wins") + "-" + stat2.getInt("looses")))), false, false);
            Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5) + (ip * 8 + 7), new ValueRange().setValues(Collections.singletonList(Collections.singletonList(stat1.getInt("wins") + "-" + stat1.getInt("looses")))), false, false);
            Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5 - 3) + (ip * 8 + 3), new ValueRange().setValues(r2), false, false);
            Google.updateRequest(sid, "Vorrunde!" + getAsXCoord(gameday * 5 - 1) + (ip * 8 + 3), new ValueRange().setValues(r1), false, false);
        }
        List<List<Object>> send1 = new ArrayList<>();
        send1.add(mons1.stream().map(Command::getIconSprite).collect(Collectors.toList()));
        Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x1 - 1) + (y1 + gameday + 3), new ValueRange().setValues(send1), false, false);
        List<List<Object>> send2 = new ArrayList<>();
        send2.add(mons2.stream().map(Command::getIconSprite).collect(Collectors.toList()));
        Google.updateRequest(sid, "Teilnehmer!" + getAsXCoord(x2 - 1) + (y2 + gameday + 3), new ValueRange().setValues(send2), false, false);
        List<List<Object>> statsend = new ArrayList<>();
        int games = obj.getInt("games");
        for (String s : statorder) {
            JSONObject o = obj.getJSONObject(s);
            int urate = o.getInt("usagerate");
            int winrate = o.getInt("winrate");
            int kills = o.getInt("kills");
            int deaths = o.getInt("deaths");
            System.out.println("s = " + s);
            statsend.add(Arrays.asList(urate, divAndRound(urate, games * 2, true), winrate, divAndRound(winrate, urate, true), kills, divAndRound(kills, urate, false), deaths, divAndRound(deaths, urate, false), kills - deaths));
        }
        Google.updateRequest(sid, "Statistiken!C3", new ValueRange().setValues(statsend), true, false);
        System.out.println("statorder1 = " + statorder);
        System.out.println("origstatorder3 = " + statorder);
        statorder.removeAll(origstatorder);
        System.out.println("statorder2 = " + statorder);
        List<List<Object>> newmons = new ArrayList<>();
        for (String s : statorder) {
            newmons.add(Arrays.asList(s, "=IMAGE(\"" + getSugiLink(s) + "\")"));
        }
        Google.updateRequest(sid, "Statistiken!A" + (origstatorder.size() + 3), new ValueRange().setValues(newmons), false, false);
        System.out.println("Updating...");
        sortBST();
    }*/
}
