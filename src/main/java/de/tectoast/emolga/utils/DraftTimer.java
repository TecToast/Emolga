package de.tectoast.emolga.utils;

import static java.util.Calendar.*;

public enum DraftTimer {
    ASL(new TimerInfo().add(10, 22, SATURDAY, SUNDAY).add(12, 22, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), 120),
    NDS(new TimerInfo().set(12, 22), 120);

    private final TimerInfo timerInfo;
    private final int delayInMins;

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
