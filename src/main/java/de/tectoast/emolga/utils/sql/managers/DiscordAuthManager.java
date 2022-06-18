package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn;
import de.tectoast.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class DiscordAuthManager extends DataManager {
    private static final Logger logger = LoggerFactory.getLogger(DiscordAuthManager.class);
    final StringColumn COOKIE = new StringColumn("hexcookie", this);
    final StringColumn REFRESHTOKEN = new StringColumn("refreshtoken", this);
    final StringColumn LASTACCESSTOKEN = new StringColumn("lastAccesstoken", this);
    final TimestampColumn TOKENEXPIRES = new TimestampColumn("tokenexpires", this);
    final LongColumn USERID = new LongColumn("userid", this);

    public DiscordAuthManager() {
        super("discordauth");
        setColumns(COOKIE, REFRESHTOKEN, LASTACCESSTOKEN, TOKENEXPIRES, USERID);
    }

    public String generateCookie(JSONObject tokens, long userid) {
        String c = COOKIE.retrieveValue(REFRESHTOKEN, tokens.getString("refresh_token"));
        if (c != null) return c;
        String cookie = new BigInteger(256, new SecureRandom()).toString(16);
        insert(cookie, tokens.getString("refresh_token"), tokens.getString("access_token"), new Timestamp(Instant.now().plusSeconds(tokens.getInt("expires_in") - 100).toEpochMilli()), userid);
        return cookie;
    }

    public ResultSet getSessionByCookie(String cookie) {
        ResultSet resultSet = readWrite(selectAll(COOKIE.check(cookie)), r -> {
            logger.info("Mapping oder so");
            return r;
        });
        try {
            logger.info("resultSet.isClosed() = " + resultSet.isClosed());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultSet;
    }
}
