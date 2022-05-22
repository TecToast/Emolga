package de.tectoast.emolga.utils;

import static java.util.Calendar.*;

public enum DraftTimer {
    ASL(new TimerInfo().add(10, 22, SATURDAY, SUNDAY).add(12, 22, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)),
    NDS(new TimerInfo().set(12, 22));

    private final TimerInfo timerInfo;
    private final int delayInMins;

    DraftTimer(TimerInfo timerInfo) {
        this.timerInfo = timerInfo;
        this.delayInMins = 120;
    }

    public TimerInfo getTimerInfo() {
        return timerInfo;
    }

    public int getDelayInMins() {
        return delayInMins;
    }
}
