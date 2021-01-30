package de.tectoast.utils;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.HashMap;

public class DexQuiz {
    public static final ArrayList<DexQuiz> list = new ArrayList<>();
    public final int cr;
    public final HashMap<Member, Integer> points = new HashMap<>();
    final TextChannel tc;
    public String gerName;
    public String englName;
    public int round = 1;
    public boolean block = false;

    public DexQuiz(TextChannel tc, String gerName, String englName, int rounds) {
        this.tc = tc;
        this.gerName = gerName;
        this.englName = englName;
        this.cr = rounds;
        if (cr <= 0) {
            tc.sendMessage("Du musst eine Mindestanzahl von einer Runde angeben!").queue();
            return;
        }
        list.add(this);
    }

    public static DexQuiz getByTC(TextChannel tc) {
        return list.stream().filter(q -> q.tc.getId().equals(tc.getId())).findFirst().orElse(null);
    }
}
