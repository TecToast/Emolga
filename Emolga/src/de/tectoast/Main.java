package de.tectoast;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.bot.EmolgaMain;

import javax.security.auth.login.LoginException;

public class Main {

    public static void main(String[] args) throws LoginException, InterruptedException {
        Command.init();
        EmolgaMain.start();
    }

}
