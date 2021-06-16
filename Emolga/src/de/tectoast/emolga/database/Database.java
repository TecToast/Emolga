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
            System.out.println("SELECT REQUEST: " + query);
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
                //Command.sendToMe("Reconnected!");
            }
            instance.lastRequest = System.currentTimeMillis();
            System.out.println("UPDATE REQUEST: " + query);
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
            //if (value instanceof String || value instanceof Timestamp) list.add("'" + value + "'");
            /*else*/
            list.add(value.toString());
        }
        query = query + String.join(", ", list) + ")";
        System.out.println("INSERT REQUEST: " + query);
        System.out.println();
        update(query);
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
        }).start();
    }

    public static void updateOrInsert(String table, String checkcolumn, String editcolumn, String checkvalue, String editvalue) {
        new Thread(() -> {
            try {
                if (select("SELECT * FROM " + table + " WHERE " + checkcolumn + " = " + checkvalue + "").next()) {
                    update("UPDATE " + table + " SET " + editcolumn + " = " + editvalue + " WHERE " + checkcolumn + " = " + checkvalue);
                } else {
                    insert(table, checkcolumn + ", " + editcolumn, checkvalue, editvalue);
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }).start();

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
                if(name.equals("analysis")) Command.updatePresence();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }).start();
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
}
