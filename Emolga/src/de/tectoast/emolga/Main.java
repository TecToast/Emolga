package de.tectoast.emolga;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.database.Database;

import javax.security.auth.login.LoginException;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws LoginException, InterruptedException, SQLException {
        Command.init();
        Database.init();
        EmolgaMain.start();
    }

}
