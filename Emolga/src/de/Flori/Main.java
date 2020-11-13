package de.Flori;

import de.Flori.Commands.Command;
import de.Flori.Emolga.EmolgaMain;

import javax.security.auth.login.LoginException;

public class Main {

    public static void main(String[] args) throws LoginException, InterruptedException {
        Command.init();
        EmolgaMain.start();
    }

}
