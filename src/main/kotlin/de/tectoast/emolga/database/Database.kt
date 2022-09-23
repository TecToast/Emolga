package de.tectoast.emolga.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.sql.managers.*
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.sql.Connection
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
        lateinit var instance: Database
        private val dbScope =
            CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("DBScope") + CoroutineExceptionHandler { _, t ->
                logger.error("ERROR IN DATABASE SCOPE", t)
                Command.sendToMe("Error in database scope, look in console")
            })


        fun init() {
            val cred = Command.tokens.getJSONObject("database")
            logger.info("Creating DataSource...")
            instance = Database(cred.getString("username"), cred.getString("password"))
            org.jetbrains.exposed.sql.Database.connect(instance.dataSource)
            logger.info("Retrieving all startup information...")
            AnalysisManager.forAll { Command.replayAnalysis[it.getLong("replay")] = it.getLong("result") }
            MusicGuildsManager.forAll { CommandCategory.musicGuilds.add(it.getLong("guildid")) }
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
                            2, EmolgaMain.emolgajda.getGuildById(Constants.G.ASL)!!
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

        val connection: Connection
            get() = instance.dataSource.connection
    }
}