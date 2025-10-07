package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.v1.core.ReferenceOption.CASCADE
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.selectAll

object WRCRunningDB : Table("wrc_running") {
    val WRCNAME = varchar("wrcname", 100)
    val WARRIORROLE = long("warriorrole")
    val GUILD = long("guild")
    val TLIDENTIFIER = varchar("tlidentifier", 100).nullable()
    val SID = varchar("sid", 100)

    suspend fun getByName(name: String) = dbTransaction {
        selectAll().where { WRCNAME eq name }.firstOrNull()
    }

    override val primaryKey = PrimaryKey(WRCNAME)
}

object WRCSignupDB : Table("wrc_signup") {
    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val USERID = long("userid")

    override val primaryKey = PrimaryKey(WRCNAME, GAMEDAY, USERID)

    init {
        foreignKey(WRCNAME to WRCRunningDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }
}

object WRCRegisteredDB : Table("wrc_registered") {
    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val USERID = long("userid")

    override val primaryKey = PrimaryKey(WRCNAME,  USERID)

    init {
        foreignKey(WRCNAME to WRCRunningDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }
}