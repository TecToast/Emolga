package de.tectoast.emolga.database;

import de.tectoast.emolga.commands.Command;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;

public class Database {

    private static Database instance;
    private Connection connection;
    private long lastRequest;
    private final String username;
    private final String password;

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
}
