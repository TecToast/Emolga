package de.tectoast.emolga.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GPIOManager {

    private static final Logger logger = LoggerFactory.getLogger(GPIOManager.class);

    private static final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    private static final int TURN_ON_TIME = 500;
    private static final int TURN_OFF_TIME = 500;
    private static final int POWER_OFF = 5000;

    private static void toggle(int duration) {
        exec(new String[]{"/usr/bin/gpio", "write", "2", "0"});
        service.schedule(() -> exec(new String[]{"/usr/bin/gpio", "write", "2", "1"}), duration, TimeUnit.MILLISECONDS);
    }

    public static void startServer() {
        toggle(TURN_ON_TIME);
    }

    public static void stopServer() {
        toggle(TURN_OFF_TIME);
    }

    public static void powerOff() {
        toggle(POWER_OFF);
    }

    private static void exec(String[] cmd) {
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isOn() throws IOException {
        return new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(new String[]{"/usr/bin/gpio", "read", "24"}).getInputStream())).readLine().trim().equals("1");
    }

}
