package de.tectoast.emolga.utils.sql.base;

@SuppressWarnings("unused")
public class Condition {

    public static String and(String con1, String con2) {
        return and(con1, con2, true);
    }

    public static String and(String con1, String con2, boolean p2) {
        return "(" + con1 + (p2 ? " AND " + con2 : "") + ")";
    }

    public static String or(String con1, String con2) {
        return or(con1, con2, true);
    }

    public static String or(String con1, String con2, boolean p2) {
        return p2 ? "(" + con1 + " OR " + con2 + ")" : con1;
    }
}
