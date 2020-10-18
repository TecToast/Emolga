package de.Flori;

import de.Flori.Commands.Command;
import de.Flori.Emolga.EmolgaMain;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws LoginException, InterruptedException {
        Command.init();
        EmolgaMain.start();
    }

}
