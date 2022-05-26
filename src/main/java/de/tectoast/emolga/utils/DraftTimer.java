package de.tectoast.emolga.utils;

import static java.util.Calendar.*;

public enum DraftTimer {
    ASL(new TimerInfo().add(10, 22, SATURDAY, SUNDAY).add(12, 22, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)),
    NDS(new TimerInfo().set(12, 22), 180);

    private static final int DEFAULT_MINS = 120;
    private final TimerInfo timerInfo;
    private final int delayInMins;

    DraftTimer(TimerInfo timerInfo) {
        this(timerInfo, DEFAULT_MINS);
    }

    DraftTimer(TimerInfo timerInfo, int delayInMins) {
        this.timerInfo = timerInfo;
        this.delayInMins = delayInMins;
    }

    public TimerInfo getTimerInfo() {
        return timerInfo;
    }

    public int getDelayInMins() {
        return delayInMins;
    }
}
