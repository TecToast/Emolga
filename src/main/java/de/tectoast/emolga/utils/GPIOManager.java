package de.tectoast.emolga.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GPIOManager {

    private static final Logger logger = LoggerFactory.getLogger(GPIOManager.class);

    private static void toggle(int duration) {
        new Thread(() -> {
            try {
                exec(new String[]{"/usr/bin/gpio", "write", "2", "0"});
                Thread.sleep(duration);
                exec(new String[]{"/usr/bin/gpio", "write", "2", "1"});
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void startServer() {
        toggle(500);
    }

    public static void hardStopServer() {
        toggle(5000);
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
