package de.tectoast.emolga.utils;

import de.tectoast.emolga.commands.dexquiz.DexQuizTip;
import de.tectoast.emolga.utils.records.DexEntry;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static de.tectoast.emolga.commands.Command.*;

public class DexQuiz {
    private static final Map<Long, DexQuiz> activeQuizzes = new HashMap<>();
    private static List<String> cachedMons;
    private final long totalRounds;
    private final Map<Long, Integer> points = new HashMap<>();
    private final TextChannel tc;
    private String gerName;
    private String englName;
    private String edition;
    private final Map<Long, Long> tipPoints = new HashMap<>();
    private boolean block = false;
    private final Random random = new Random();
    private long round = 1;
    private long totalbudget;

    public DexQuiz(TextChannel tc, long rounds) {
        this.tc = tc;
        this.totalRounds = rounds;
        if (totalRounds <= 0) {
            tc.sendMessage("Du musst eine Mindestanzahl von einer Runde angeben!").queue();
            return;
        }
        activeQuizzes.put(tc.getIdLong(), this);
        long gid = tc.getGuild().getIdLong();
        totalbudget = (int) ConfigManager.DEXQUIZ.getValue(gid, "totalbudget") * this.totalRounds;
        EmbedBuilder b = new EmbedBuilder()
                .setTitle("Mögliche Tipps")
                .setDescription("Alle möglichen Tipps mit Preisen aufgelistet, konfigurierbar mit `/configurate dexquiz`")
                .setColor(Color.CYAN)
                .addField("Punkte-Budget", String.valueOf(totalbudget), false);
        DexQuizTip.buildEmbedFields(gid).forEach(b::addField);
        tc.sendMessageEmbeds(b.build()).queue();
        newMon(false);
    }

    public static DexQuiz getByTC(TextChannel tc) {
        return getByTC(tc.getIdLong());
    }

    public static DexQuiz getByTC(long tcid) {
        return activeQuizzes.get(tcid);
    }

    public long useTip(long user, String tipName) {
        long gid = tc.getGuild().getIdLong();
        int price = (int) ConfigManager.DEXQUIZ.getValue(gid, tipName);
        if (price == -1) return -10;
        if (!tipPoints.containsKey(user)) tipPoints.put(user, totalbudget);
        if (tipPoints.get(user) - price < 0) return -1;
        return tipPoints.compute(user, (k, i) -> i - price);
    }

    public @Nullable Pair<String, String> getNewMon() {
        try {
            if (cachedMons == null) {
                File file = new File("./entwicklung.txt");
                cachedMons = Files.readAllLines(file.toPath());
            }
            String pokemon = cachedMons.get(random.nextInt(cachedMons.size()));
            String englName = getEnglName(pokemon);
            return new ImmutablePair<>(pokemon, englName);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return null;
    }

    public boolean check(String t) {
        return t.equalsIgnoreCase(gerName) || t.equalsIgnoreCase(englName);
    }

    public void end() {
        activeQuizzes.remove(tc.getIdLong());
        if (points.isEmpty() && round > 1) {
            tc.sendMessage("Nur den Solution Command zu benutzen ist nicht der Sinn der Sache! xD").queue();
            return;
        }
        StringBuilder builder = new StringBuilder("Punkte:\n");
        //noinspection SuspiciousMethodCalls
        for (long mem : points.keySet().stream().sorted(Comparator.comparing(points::get).reversed()).toList()) {
            builder.append("<@").append(mem).append(">").append(": ").append(points.get(mem)).append("\n");
        }
        tc.sendMessage(builder.toString()).queue();
    }

    public void nextRound() {
        incrementRound();
        if (isEnded()) {
            end();
            return;
        }
        newMon();
    }

    public void newMon() {
        newMon(true);
    }

    public void newMon(boolean withDelay) {
        Pair<String, String> mon = getNewMon();
        String pokemon = mon.getLeft();
        String englName = mon.getRight();
        DexEntry dexEntry = DBManagers.POKEDEX.getDexEntry(pokemon);
        String entry = dexEntry.entry();
        edition = dexEntry.edition();
        gerName = pokemon;
        sendDexEntry(this.tc.getAsMention() + " " + pokemon);
        this.englName = englName;
        //ü = %C3%B6
        this.block = false;
        MessageAction ma = this.tc.sendMessage("Runde %d/%d: %s\nZu welchem Pokemon gehört dieser Dex-Eintrag?".formatted(round, totalRounds, trim(entry, pokemon)));
        if (withDelay)
            ma.queueAfter(3, TimeUnit.SECONDS);
        else ma.queue();
    }

    public void givePoint(long member) {
        points.compute(member, (key, oldvalue) -> oldvalue == null ? 1 : oldvalue + 1);
    }

    public boolean isEnded() {
        return round > totalRounds;
    }

    public String getCurrentGerName() {
        return gerName;
    }

    public String getCurrentEnglName() {
        return englName;
    }

    public String getCurrentEdition() {
        return edition;
    }

    public long getRound() {
        return round;
    }

    public boolean nonBlocking() {
        return !block;
    }

    public void block() {
        block = true;
    }

    public void incrementRound() {
        round++;
    }
}
