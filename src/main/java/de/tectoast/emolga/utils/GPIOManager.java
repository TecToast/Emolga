package de.tectoast.emolga.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GPIOManager {

    private static final Logger logger = LoggerFactory.getLogger(GPIOManager.class);

    private static final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    private static final int TURN_ON_TIME = 500;
    private static final int TURN_OFF_TIME = 500;
    private static final int POWER_OFF = 5000;

    private static void toggle(PC pc, int duration) {
        exec(new String[]{"/usr/bin/gpio", "write", pc.getWritePin(), "0"});
        service.schedule(() -> exec(new String[]{"/usr/bin/gpio", "write", pc.getWritePin(), "1"}), duration, TimeUnit.MILLISECONDS);
    }

    public static void startServer(PC pc) {
        toggle(pc, TURN_ON_TIME);
    }

    public static void stopServer(PC pc) {
        toggle(pc, TURN_OFF_TIME);
    }

    public static void powerOff(PC pc) {
        toggle(pc, POWER_OFF);
    }

    private static void exec(String[] cmd) {
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isOn(PC pc) throws IOException {
        return new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(new String[]{"/usr/bin/gpio", "read", pc.getReadPin()}).getInputStream())).readLine().trim().equals("1");
    }

    public enum PC {
        FLORIX_2(2, 24, 964571226964115496L),
        FLORIX_3(3, 25, 975076826588282962L);

        private final int writePin;
        private final int readPin;
        private final long messageId;

        PC(int writePin, int readPin, long messageId) {
            this.writePin = writePin;
            this.readPin = readPin;
            this.messageId = messageId;
        }

        public static PC byMessage(long messageId) {
            return Arrays.stream(values()).filter(pc -> pc.messageId == messageId).findFirst().orElse(null);
        }

        public String getWritePin() {
            return String.valueOf(writePin);
        }

        public String getReadPin() {
            return String.valueOf(readPin);
        }
    }

}
