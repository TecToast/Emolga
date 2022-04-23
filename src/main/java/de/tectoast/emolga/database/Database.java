package de.tectoast.emolga.database;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.sql.DBManagers;
import de.tectoast.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;

import static de.tectoast.emolga.commands.Command.replayAnalysis;
import static de.tectoast.emolga.commands.Command.spoilerTags;

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
                ResultSet spoiler = Database.select("select * from spoilertags");
                while (spoiler.next()) {
                    spoilerTags.add(spoiler.getLong("guildid"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, "DBCreation").start();
    }

    public static void init() {
        JSONObject cred = Command.tokens.getJSONObject("database");
        instance = new Database(cred.getString("username"), cred.getString("password"));
    }

    public static ResultSet select(String query, boolean suppressMessage) {
        try {
            if (System.currentTimeMillis() - instance.lastRequest >= 3600000) {
                instance.connection.close();
                instance.connection = DriverManager.getConnection("jdbc:mysql://localhost/emolga?autoReconnect=true", instance.properties);
            }
            instance.lastRequest = System.currentTimeMillis();
            if (!suppressMessage) logger.info("SELECT REQUEST: " + query);
            return instance.connection.createStatement().executeQuery(query);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    public static ResultSet select(String query) {
        return select(query, false);
    }

    public static int update(String query) {
        try {
            if (System.currentTimeMillis() - instance.lastRequest >= 3600000) {
                instance.connection.close();
                instance.connection = DriverManager.getConnection("jdbc:mysql://localhost/emolga", instance.properties);
                //Command.sendToMe("Reconnected!");
            }
            instance.lastRequest = System.currentTimeMillis();
            logger.info("UPDATE REQUEST: " + query);
            return instance.connection.createStatement().executeUpdate(query);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return -1;
    }

    public static void incrementPredictionCounter(long userid) {
        new Thread(() -> {
            try {
                PreparedStatement usernameInput = instance.connection.prepareStatement("SELECT userid FROM predictiongame WHERE userid = ? ");
                usernameInput.setLong(1, userid);
                if (usernameInput.executeQuery().next()) {
                    update("UPDATE predictiongame SET predictions = (predictions + 1) WHERE userid = " + userid);
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


    public static void incrementStatistic(String name) {
        new Thread(() -> {
            try {
                PreparedStatement usernameInput = instance.connection.prepareStatement("SELECT name FROM statistics WHERE name = ? ");
                usernameInput.setString(1, name);
                if (usernameInput.executeQuery().next()) {
                    update("UPDATE statistics SET count = (count + 1) WHERE name = \"" + name + "\"");
                } else {
                    PreparedStatement userDataInput = instance.connection.prepareStatement("INSERT INTO statistics (name,count) VALUES (?,?);");
                    userDataInput.setString(1, name);
                    userDataInput.setInt(2, 1);
                    userDataInput.executeUpdate();
                }
                if (name.equals("analysis")) Command.updatePresence();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }, "IncrStat").start();
    }

    public static String getDescriptionFrom(String type, String id) {
        ResultSet set = select("SELECT description from " + type + "data WHERE name = \"" + id + "\"");
        try {
            set.next();
            return set.getString("description");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    private static String getValue(Object o) {
        if (o instanceof String) return "\"" + o + "\"";
        return o.toString();
    }

    public static Object getData(String table, String desiredcolumn, String checkcolumn, Object checkvalue) {
        ResultSet set = select("SELECT " + desiredcolumn + " from " + table + " WHERE " + checkcolumn + " = " + getValue(checkvalue));
        try {
            set.next();
            return set.getObject(desiredcolumn);
        } catch (SQLException throwables) {
            //throwables.printStackTrace();
        }
        return null;
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
