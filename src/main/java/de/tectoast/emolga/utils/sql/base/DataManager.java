package de.tectoast.emolga.utils.sql.base;

import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.sql.base.columns.SQLColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SuppressWarnings("SameParameterValue")
public abstract class DataManager {
    private static final Logger logger = LoggerFactory.getLogger(DataManager.class);
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

    public static int executeUpdate(String query) {
        try {
            Statement statement = getConnection().createStatement();
            if (query.startsWith("INSERT INTO") || query.startsWith("UPDATE") || query.startsWith("DELETE")) {
                logger.info("query = " + query);
            }
            return statement.executeUpdate(query);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return -1;
        }
    }

    protected static SelectBuilder selectBuilder() {
        return new SelectBuilder();
    }

    protected static Connection getConnection() {
        return Database.getConnection();
    }

    public static void read(String query, ResultsConsumer rc) {
        try {
            Statement statement = getConnection().createStatement();
            ResultSet results = statement.executeQuery(query);
            rc.consume(results);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static <T> T read(String query, ResultsFunction<T> rf) {
        return read(query, rf, null);
    }

    public static <T> T read(String query, ResultsFunction<T> rf, T err) {
        try {
            Statement statement = getConnection().createStatement();
            ResultSet results = statement.executeQuery(query);
            return rf.apply(results);
        } catch (SQLException e) {
            e.printStackTrace();
            return err;
        }
    }

    public static void readWrite(String query, ResultsConsumer rc) {
        try {
            Statement statement = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            ResultSet results = statement.executeQuery(query);
            rc.consume(results);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static <T> T readWrite(String query, ResultsFunction<T> rf, T err) {
        try {
            ResultSet set = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE).executeQuery(query);
            return rf.apply(set);
        } catch (SQLException e) {
            return err;
        }
    }

    public static <T> T readWrite(String query, ResultsFunction<T> rf) {
        return readWrite(query, rf, null);
    }

    public static <T> List<T> map(ResultSet set, Function<ResultSet, T> mapper) {
        List<T> l = new LinkedList<>();
        try {
            while (set.next()) {
                l.add(mapper.apply(set));
            }
            set.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return l;
    }

    public static <T> T mapFirst(ResultSet set, Function<ResultSet, T> mapper, T orElse) {
        try {
            if (set.next()) {
                T val = mapper.apply(set);
                set.close();
                return val;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return orElse;
    }

    public static Integer unwrapCount(ResultSet set, String alias) {
        try {
            return set.getInt(alias);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void forEach(ResultSet set, Consumer<ResultSet> consumer) {
        try {
            while (set.next()) {
                consumer.accept(set);
            }
            set.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void update(SQLColumn<?> checkcolumn, Object checkvalue, SQLColumn<?> updateColumn, Object updateValue) {
        executeUpdate("UPDATE " + tableName + " SET " + updateColumn.check(updateValue) + " WHERE " + checkcolumn.check(checkvalue));
    }

    public void forAll(ResultsConsumer c) {
        read(selectAll(), rc -> {
            while (rc.next()) {
                c.consume(rc);
            }
        });
    }

    public void update(SQLColumn<?> checkcolumn, Object checkvalue, SQLColumn<?>[] updateColumn, Object... updateValue) {
        List<String> l = new LinkedList<>();
        for (int i = 0; i < updateColumn.length; i++) {
            l.add(updateColumn[i].check(updateValue[i]));
        }
        executeUpdate("UPDATE " + tableName + " SET " + String.join(",", l) + " WHERE " + checkcolumn.check(checkvalue));
    }

    public void addStatistics(String id, Object... defaultValues) {
        addStatisticsSpecified(id, Arrays.asList(columns), defaultValues);
    }

    public void addStatisticsSpecified(String id, List<SQLColumn<?>> cols, Object... defaultValues) {
        insertOrUpdate(IntStream.range(1, cols.size()).boxed().collect(Collectors.toMap(
                cols::get, k -> s -> s + "+" + defaultValues[k - 1]
        )), Stream.concat(Stream.of(id), Arrays.stream(defaultValues)).toArray());
    }

    public void replaceIfExists(Object... values) {
        insertOrUpdate(Collections.emptyMap(), values);
    }

    public void insertOrUpdate(Map<SQLColumn<?>, Function<String, String>> map, Object... defaultValues) {
        insertOrUpdate(map, Arrays.asList(columns), defaultValues);
    }

    public void insertOrUpdate(Map<SQLColumn<?>, Function<String, String>> map, List<SQLColumn<?>> cols, Object... defaultValues) {
        try {
            if (!ready) throw new IllegalStateException("DataManager " + this.tableName + " is not initialized!");
            Statement stmt = getConnection().createStatement();
            String q = "INSERT INTO %s VALUES (%s) ON DUPLICATE KEY UPDATE %s".formatted(
                    tableName,
                    generateInsertString(defaultValues),
                    IntStream.range(1, cols.size())
                            .mapToObj(num -> {
                                SQLColumn<?> sc = cols.get(num);
                                return "%s=%s".formatted(sc.name, map.getOrDefault(sc, str -> sc.wrap(defaultValues[num])).apply(sc.name));
                            })
                            .collect(Collectors.joining(", "))
            );
            logger.info(MarkerFactory.getMarker("important"), "q = {}", q);
            stmt.executeUpdate(q);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insert(Object... newValues) {
        if (!ready) throw new IllegalStateException("DataManager " + this.tableName + " is not initialized!");
        executeUpdate("INSERT INTO %s (%s) VALUES (%s)".formatted(tableName, Arrays.stream(columns).map(s -> s.name).collect(Collectors.joining(", ")), generateInsertString(newValues)));
    }

    public String generateInsertString(Object... newValues) {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < columns.length; i++) {
            Object newValue = newValues[i];
            if (newValue == null) l.add("NULL");
            else
                l.add(columns[i].wrap(newValue));
        }
        return String.join(", ", l);
    }

    public final String deleteStmt(String where) {
        return "DELETE FROM " + this.tableName + " WHERE " + where;
    }

    public final int delete(String where) {
        return executeUpdate(deleteStmt(where));
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

    protected static class SelectBuilder {
        String columns;
        String where;
        String orderBy;

        public SelectBuilder columns(SQLColumn<?>... columns) {
            this.columns = Arrays.stream(columns).map(s -> s.name).collect(Collectors.joining(", "));
            return this;
        }

        public SelectBuilder count(String alias) {
            this.columns = "count(*) as " + alias;
            return this;
        }

        public SelectBuilder where(String where) {
            this.where = where;
            return this;
        }

        public SelectBuilder orderBy(SQLColumn<?> orderBy, String direction) {
            this.orderBy = orderBy.name + " " + direction;
            return this;
        }

        public String build(DataManager dm) {
            return "SELECT %s FROM %s%s%s".formatted(
                    columns == null ? "*" : columns,
                    dm.tableName,
                    where == null ? "" : " WHERE " + where,
                    orderBy == null ? "" : " ORDER BY " + orderBy
            );
        }
    }


    @FunctionalInterface
    public interface ResultsConsumer {
        void consume(ResultSet results) throws SQLException;
    }

    @FunctionalInterface
    public interface ResultsFunction<T> {
        T apply(ResultSet results) throws SQLException;
    }

}
