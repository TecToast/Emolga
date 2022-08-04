package de.tectoast.emolga.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.sql.managers.*
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

class Database(username: String, password: String) {
    val dataSource: DataSource

    init {
        //dataSource = new MariaDbPoolDataSource("jdbc:mariadb://localhost/emolga?user=%s&password=%s&minPoolSize=1".formatted(username, password));
        val conf = HikariConfig()
        conf.jdbcUrl = "jdbc:mariadb://localhost/emolga?user=$username&password=$password&minPoolSize=1"
        dataSource = HikariDataSource(conf)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Database::class.java)
        private var instance: Database? = null
        private val dbScope = CoroutineScope(Dispatchers.IO + CoroutineName("DBScope"))

        @JvmStatic
        fun init() {
            val cred = Command.tokens.getJSONObject("database")
            logger.info("Creating DataSource...")
            instance = Database(cred.getString("username"), cred.getString("password"))
            logger.info("Retrieving all startup information...")
            AnalysisManager.forAll { r: ResultSet ->
                Command.replayAnalysis[r.getLong("replay")] = r.getLong("result")
            }
            MusicGuildsManager.forAll { r -> CommandCategory.musicGuilds.add(r.getLong("guildid")) }
            CalendarManager.allEntries.forEach { Command.scheduleCalendarEntry(it) }
            logger.info("replayAnalysis.size() = " + Command.replayAnalysis.size)
            SpoilerTagsManager.addToList()
        }


        fun incrementPredictionCounter(userid: Long) {
            dbScope.launch {
                try {
                    val conn = connection
                    val usernameInput = conn.prepareStatement("SELECT userid FROM predictiongame WHERE userid = ? ")
                    usernameInput.setLong(1, userid)
                    if (usernameInput.executeQuery().next()) {
                        PredictionGameManager.addPoint(userid)
                    } else {
                        val userDataInput =
                            conn.prepareStatement("INSERT INTO predictiongame (userid, username, predictions) VALUES (?,?,?);")
                        userDataInput.setLong(1, userid)
                        userDataInput.setString(
                            2, EmolgaMain.emolgajda.getGuildById(Constants.ASLID)!!
                                .retrieveMemberById(userid).await().effectiveName
                        )
                        userDataInput.setInt(3, 1)
                        userDataInput.executeUpdate()
                        userDataInput.close()
                    }
                    usernameInput.close()
                    conn.close()
                } catch (throwables: SQLException) {
                    throwables.printStackTrace()
                }
            }
        }

        @JvmStatic
        val connection: Connection
            get() = try {
                instance!!.dataSource.connection
            } catch (e: SQLException) {
                throw RuntimeException(e)
            }
    }
}