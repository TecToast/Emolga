package de.tectoast.emolga.utils;

import java.util.HashMap;
import java.util.Map;

public class TimerInfo {

    private final Map<Integer, TimerData> map = new HashMap<>();

    public TimerInfo add(int from, int to, int... days) {
        for (int day : days) {
            map.put(day, new TimerData(from, to));
        }
        return this;
    }

    public TimerInfo set(int from, int to) {
        for (int i = 1; i <= 7; i++) {
            map.put(i, new TimerData(from, to));
        }
        return this;
    }

    public TimerData get(int day) {
        if (!map.containsKey(day)) throw new IllegalStateException("TimerInfo Map Incomplete (" + day + ")");
        return map.get(day);
    }

    public static record TimerData(int from, int to) {
    }
}
