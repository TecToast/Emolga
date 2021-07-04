package de.tectoast.emolga.utils.sql.base;

import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.sql.base.columns.SQLColumn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public abstract class DataManager {
    private final String tableName;
    protected SQLColumn<?>[] columns;
    private boolean ready = false;

    public DataManager(String tableName) {
        this.tableName = tableName;
    }

    protected void setColumns(SQLColumn<?>... columns) {
        this.columns = columns;
        ready = true;
    }

    public void insertOrUpdate(SQLColumn<?> checkcolumn, Object checkvalue, Object... newValues) {
        if (!ready) throw new IllegalStateException("DataManager " + this.getTableName() + " is not initialized!");
        readWrite(selectAll(checkcolumn.check(checkvalue)), res -> {
            if (res.next()) {
                for (int i = 0; i < columns.length; i++) {
                    SQLColumn<?> sc = columns[i];
                    Object value = newValues[i];
                    sc.update(res, value);
                }
                res.updateRow();
            } else {
                res.moveToInsertRow();
                for (int i = 0; i < columns.length; i++) {
                    SQLColumn<?> sc = columns[i];
                    Object value = newValues[i];
                    sc.update(res, value);
                }
                res.insertRow();
            }
        });
    }

    public void editOneValue(SQLColumn<?> checkcolumn, Object checkvalue, SQLColumn<?> toedit, Object editobject) {
        readWrite(selectAll(checkcolumn.check(checkvalue)), res -> {
            if (res.next()) {
                toedit.update(res, editobject);
                res.updateRow();
            }
        });
    }

    public void forAll(ResultsConsumer c) {
        read(selectAll(), rc -> {
            while (rc.next()) {
                c.consume(rc);
            }
        });
    }


    public void insert(Object... newValues) {
        if (!ready) throw new IllegalStateException("DataManager " + this.getTableName() + " is not initialized!");
        ArrayList<String> l = new ArrayList<>();
        for (int i = 0; i < columns.length; i++) {
            Object newValue = newValues[i];
            if (newValue == null) l.add("NULL");
            else
                l.add(columns[i].wrap(newValue));
        }
        Database.update("INSERT INTO " + getTableName() + " (" + Arrays.stream(columns).map(s -> s.name).collect(Collectors.joining(", ")) + ") VALUES (" + String.join(", ", l) + ")");
    }

    public final void read(String query, ResultsConsumer rc) {
        try (Statement statement = getConnection().createStatement();
             ResultSet results = statement.executeQuery(query)) {
            rc.consume(results);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public final <T> T read(String query, ResultsFunction<T> rf) {
        return read(query, rf, null);
    }

    public final <T> T read(String query, ResultsFunction<T> rf, T err) {
        try (Statement statement = getConnection().createStatement();
             ResultSet results = statement.executeQuery(query)) {
            return rf.apply(results);
        } catch (SQLException e) {
            e.printStackTrace();
            return err;
        }
    }

    public final void readWrite(String query, ResultsConsumer rc) {
        try (Statement statement = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
             ResultSet results = statement.executeQuery(query)) {
            rc.consume(results);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public final <T> T readWrite(String query, ResultsFunction<T> rf) {
        return readWrite(query, rf, null);
    }

    public final <T> T readWrite(String query, ResultsFunction<T> rf, T err) {
        try (Statement statement = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
             ResultSet results = statement.executeQuery(query)) {
            return rf.apply(results);
        } catch (SQLException e) {
            return err;
        }
    }

    public final String getTableName() {
        return tableName;
    }

    protected final Connection getConnection() {
        return Database.getConnection();
    }

    public final String select(String where, SQLColumn<?>... columns) {
        return select(where, Arrays.stream(columns).map(s -> s.name).collect(Collectors.joining(", ")));
    }

    public final String selectAll(String where) {
        return select(where, "*");
    }

    public final String selectAll() {
        return select(null, "*");
    }

    public String select(String where, String columns) {
        return "SELECT " + columns + " FROM " + tableName + (where == null ? "" : " WHERE " + where);
    }

    /*public InsertBuilder insertBuilder() {
        return new InsertBuilder();
    }*/

    @FunctionalInterface
    public interface ResultsConsumer {
        void consume(ResultSet results) throws SQLException;
    }

    @FunctionalInterface
    public interface ResultsFunction<T> {
        T apply(ResultSet results) throws SQLException;
    }

    /*public class InsertBuilder {
        LinkedList<String> columns = new LinkedList<>();
        LinkedList<String> values = new LinkedList<>();

        public InsertBuilder add(SQLColumn<?> sc, Object value) {
            columns.add(sc.name);
            values.add(sc.wrap(value));
            return this;
        }

        public InsertBuilder addNullable(SQLColumn<?> sc, Object value) {
            if (value == null) return this;
            columns.add(sc.name);
            values.add(sc.wrap(value));
            return this;
        }

        public void execute() {
            Database.update("INSERT INTO " + DataManager.this.tableName + " (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", values) + ")");
        }

    }*/
}
