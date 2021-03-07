package de.tectoast.emolga.database;

import com.mysql.cj.jdbc.Driver;
import de.tectoast.emolga.commands.Command;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Database {

    private static Database instance;
    private final Connection connection;

    public Database(String username, String password) throws SQLException {
        DriverManager.registerDriver(new Driver());
        connection = DriverManager.getConnection("jdbc:mysql://localhost/emolga", username, password);
    }

    public static void init() throws SQLException {
        JSONObject cred = Command.tokens.getJSONObject("database");
        instance = new Database(cred.getString("username"), cred.getString("password"));
    }

    public static ResultSet select(String query) {
        try {
            return instance.connection.createStatement().executeQuery(query);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    public static int update(String query) {
        try {
            return instance.connection.createStatement().executeUpdate(query);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return -1;
    }

    public static int insert(String table, String columns, Object... values) {
        String query = "insert into " + table + " (" + columns + ") values (";
        ArrayList<String> list = new ArrayList<>();
        for (Object value : values) {
            if(value instanceof String || value instanceof Timestamp) list.add("'" + value + "'");
            else list.add(value.toString());
        }
        return update(query + String.join(", ", list) + ")");
    }
}
