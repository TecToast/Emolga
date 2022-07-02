package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.columns.LongColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn
import de.tectoast.jsolf.JSONObject
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.security.SecureRandom
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant

object DiscordAuthManager : DataManager("discordauth") {
    private val COOKIE = StringColumn("hexcookie", this)
    private val REFRESHTOKEN = StringColumn("refreshtoken", this)
    private val LASTACCESSTOKEN = StringColumn("lastAccesstoken", this)
    private val TOKENEXPIRES = TimestampColumn("tokenexpires", this)
    private val USERID = LongColumn("userid", this)

    init {
        setColumns(COOKIE, REFRESHTOKEN, LASTACCESSTOKEN, TOKENEXPIRES, USERID)
    }

    fun generateCookie(tokens: JSONObject, userid: Long): String {
        val c = COOKIE.retrieveValue(REFRESHTOKEN, tokens.getString("refresh_token"))
        if (c != null) return c
        val cookie = BigInteger(256, SecureRandom()).toString(16)
        insert(
            cookie,
            tokens.getString("refresh_token"),
            tokens.getString("access_token"),
            Timestamp(Instant.now().plusSeconds((tokens.getInt("expires_in") - 100).toLong()).toEpochMilli()),
            userid
        )
        return cookie
    }

    fun getSessionByCookie(cookie: String): ResultSet {
        val resultSet = readWrite<ResultSet>(selectAll(COOKIE.check(cookie))) { r: ResultSet ->
            logger.info("Mapping oder so")
            r
        }
        try {
            logger.info("resultSet.isClosed() = " + resultSet.isClosed)
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return resultSet
    }

    private val logger = LoggerFactory.getLogger(DiscordAuthManager::class.java)
}