package de.tectoast.emolga.database;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.sql.DBManagers;
import de.tectoast.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import static de.tectoast.emolga.commands.Command.replayAnalysis;

public class Database {

    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static Database instance;
    private final Properties properties = new Properties();
    private Connection connection;
    private long lastRequest;

    public Database(String username, String password) {
        new Thread(() -> {
            try {
                logger.info("Connecting...");
                properties.setProperty("user", username);
                properties.setProperty("password", password);
                properties.setProperty("autoReconnect", "true");
                long l = System.currentTimeMillis();
                connection = DriverManager.getConnection("jdbc:mysql://localhost/emolga", properties);
                logger.info("Connected! {}", (System.currentTimeMillis() - l));
                DBManagers.ANALYSIS.forAll(r -> replayAnalysis.put(r.getLong("replay"), r.getLong("result")));
                DBManagers.MUSIC_GUILDS.forAll(r -> CommandCategory.musicGuilds.add(r.getLong("guildid")));
                DBManagers.CALENDAR.getAllEntries().forEach(Command::scheduleCalendarEntry);
                logger.info("replayAnalysis.size() = " + replayAnalysis.size());
                DBManagers.SPOILER_TAGS.addToList();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, "DBCreation").start();
    }

    public static void init() {
        JSONObject cred = Command.tokens.getJSONObject("database");
        instance = new Database(cred.getString("username"), cred.getString("password"));
    }

    public static void incrementPredictionCounter(long userid) {
        new Thread(() -> {
            try {
                PreparedStatement usernameInput = instance.connection.prepareStatement("SELECT userid FROM predictiongame WHERE userid = ? ");
                usernameInput.setLong(1, userid);
                if (usernameInput.executeQuery().next()) {
                    DBManagers.PREDICTION_GAME.addPoint(userid);
                } else {
                    PreparedStatement userDataInput = instance.connection.prepareStatement("INSERT INTO predictiongame (userid, username, predictions) VALUES (?,?,?);");
                    userDataInput.setLong(1, userid);
                    userDataInput.setString(2, EmolgaMain.emolgajda.getGuildById(Constants.ASLID).retrieveMemberById(userid).complete().getEffectiveName());
                    userDataInput.setInt(3, 1);
                    userDataInput.executeUpdate();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }, "IncrPredCounter").start();
    }


    public static void updateConnection() {
        if (System.currentTimeMillis() - instance.lastRequest >= 3600000) {
            try {
                instance.connection.close();
                instance.connection = DriverManager.getConnection("jdbc:mysql://localhost/emolga", instance.properties);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        instance.lastRequest = System.currentTimeMillis();
    }

    public static Connection getConnection() {
        updateConnection();
        return instance.connection;
    }
}
