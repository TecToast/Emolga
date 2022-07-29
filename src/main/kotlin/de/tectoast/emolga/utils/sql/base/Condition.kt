package de.tectoast.emolga.utils.sql.base

object Condition {
    fun and(con1: String, con2: String, p2: Boolean = true): String {
        return "(" + con1 + (if (p2) " AND $con2" else "") + ")"
    }

    fun or(con1: String, con2: String, p2: Boolean = true): String {
        return if (p2) "($con1 OR $con2)" else con1
    }
}