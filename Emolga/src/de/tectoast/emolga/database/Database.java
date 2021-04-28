package de.tectoast.emolga.database;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.utils.Constants;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;

public class Database {

    private static Database instance;
    private final String username;
    private final String password;
    private Connection connection;
    private long lastRequest;

    public Database(String username, String password) throws SQLException {
        System.out.println("Connecting...");
        this.username = username;
        this.password = password;
        connection = DriverManager.getConnection("jdbc:mysql://localhost/emolga?autoReconnect=true", username, password);
        System.out.println("Connected!");
    }

    public static void init() throws SQLException {
        JSONObject cred = Command.tokens.getJSONObject("database");
        instance = new Database(cred.getString("username"), cred.getString("password"));
    }

    public static ResultSet select(String query) {
        try {
            if (System.currentTimeMillis() - instance.lastRequest >= 3600000) {
                instance.connection.close();
                instance.connection = DriverManager.getConnection("jdbc:mysql://localhost/emolga?autoReconnect=true", instance.username, instance.password);
            }
            instance.lastRequest = System.currentTimeMillis();
            return instance.connection.createStatement().executeQuery(query);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    public static int update(String query) {
        try {
            if (System.currentTimeMillis() - instance.lastRequest >= 3600000) {
                instance.connection.close();
                instance.connection = DriverManager.getConnection("jdbc:mysql://localhost/emolga?autoReconnect=true", instance.username, instance.password);
                Command.sendToMe("Reconnected!");
            }
            instance.lastRequest = System.currentTimeMillis();
            return instance.connection.createStatement().executeUpdate(query);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return -1;
    }

    public static void insert(String table, String columns, Object... values) {
        String query = "insert into " + table + " (" + columns + ") values (";
        ArrayList<String> list = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof String || value instanceof Timestamp) list.add("'" + value + "'");
            else list.add(value.toString());
        }
        update(query + String.join(", ", list) + ")");
    }

    public static void incrementPredictionCounter(long userid) {
        new Thread(() -> {
            try {
                String verifyIfUserAlreadyExists = "SELECT userid FROM predictiongame WHERE userid = ? ";
                PreparedStatement usernameInput = instance.connection.prepareStatement(verifyIfUserAlreadyExists);
                usernameInput.setLong(1, userid);
                ResultSet rS = usernameInput.executeQuery();
                if (rS.next()) {
                    update("UPDATE predictiongame SET predictions = (predictions + 1) WHERE userid = " + userid);
                } else {
                    String name = EmolgaMain.jda.getGuildById(Constants.ASLID).retrieveMemberById(userid).complete().getEffectiveName();
                    PreparedStatement userDataInput = instance.connection.prepareStatement("INSERT INTO predictiongame (userid, username, predictions) VALUES (?,?,?);");
                    userDataInput.setLong(1, userid);
                    userDataInput.setString(2, name);
                    userDataInput.setInt(3, 1);
                    userDataInput.executeUpdate();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }).start();
    }
}
