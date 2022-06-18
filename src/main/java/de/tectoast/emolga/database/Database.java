package de.tectoast.emolga.database;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.sql.DBManagers;
import de.tectoast.jsolf.JSONObject;
import org.mariadb.jdbc.MariaDbPoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static de.tectoast.emolga.commands.Command.replayAnalysis;

public class Database {

    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static Database instance;
    private long lastRequest;
    MariaDbPoolDataSource dataSource;

    public Database(String username, String password) {
        try {
            dataSource = new MariaDbPoolDataSource("jdbc:mariadb://localhost/emolga?user=%s&password=%s".formatted(username, password));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {
        JSONObject cred = Command.tokens.getJSONObject("database");
        logger.info("Creating DataSource...");
        instance = new Database(cred.getString("username"), cred.getString("password"));
        logger.info("Retrieving all startup information...");
        DBManagers.ANALYSIS.forAll(r -> replayAnalysis.put(r.getLong("replay"), r.getLong("result")));
        DBManagers.MUSIC_GUILDS.forAll(r -> CommandCategory.musicGuilds.add(r.getLong("guildid")));
        DBManagers.CALENDAR.getAllEntries().forEach(Command::scheduleCalendarEntry);
        logger.info("replayAnalysis.size() = " + replayAnalysis.size());
        DBManagers.SPOILER_TAGS.addToList();
    }

    public static void incrementPredictionCounter(long userid) {
        new Thread(() -> {
            try {
                PreparedStatement usernameInput = getConnection().prepareStatement("SELECT userid FROM predictiongame WHERE userid = ? ");
                usernameInput.setLong(1, userid);
                if (usernameInput.executeQuery().next()) {
                    DBManagers.PREDICTION_GAME.addPoint(userid);
                } else {
                    PreparedStatement userDataInput = getConnection().prepareStatement("INSERT INTO predictiongame (userid, username, predictions) VALUES (?,?,?);");
                    userDataInput.setLong(1, userid);
                    userDataInput.setString(2, EmolgaMain.emolgajda.getGuildById(Constants.ASLID).retrieveMemberById(userid).complete().getEffectiveName());
                    userDataInput.setInt(3, 1);
                    userDataInput.executeUpdate();
                    userDataInput.close();
                }
                usernameInput.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }, "IncrPredCounter").start();
    }

    public static Connection getConnection() {
        try {
            return instance.dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
