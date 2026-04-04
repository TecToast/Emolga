package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.Database
import de.tectoast.emolga.features.flo.SDNamesApprovalButton
import de.tectoast.emolga.utils.toUsername
import de.tectoast.generic.K18n_No
import de.tectoast.generic.K18n_Yes
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

@Single
class SDNamesDB : Table("sdnames") {
    val NAME = varchar("name", 18)
    val ID = long("id")

    override val primaryKey = PrimaryKey(NAME)
}

interface SDNamesRepository {
    suspend fun getIDByName(name: String): Long
    fun addIfAbsent(name: String, id: Long): Deferred<SDInsertStatus>
    suspend fun setOwnerOfName(username: String, id: Long)
}

interface SDNamesNotificationService {
    suspend fun sendApprovalNotification(name: String, username: String, id: Long, currentOwner: Long)
}

@Single
class PostgresSDNamesRepository(
    val db: R2dbcDatabase,
    val sdNames: SDNamesDB,
    val notificationService: SDNamesNotificationService
) : SDNamesRepository {

    /**
     * Get a discord user id by the showdown name
     * @param name the showdown name to look for
     * @return the corresponding user id, or -1 if no user could be found
     */
    override suspend fun getIDByName(name: String) =
        suspendTransaction(db) {
            sdNames.selectAll().where { sdNames.NAME eq name.toUsername() }.firstOrNull()?.get(sdNames.ID)
        } ?: -1

    /**
     * Adds a showdown name/id combination to the database (if the name is not already used)
     * @param name the showdown name
     * @param id the discord user id
     * @return a Deferred resolvig to a [SDInsertStatus] showing the result of the operation
     */
    override fun addIfAbsent(name: String, id: Long): Deferred<SDInsertStatus> {
        return Database.dbScope.async {
            suspendTransaction(db) {
                val username = name.toUsername()
                val existing = sdNames.selectAll().where { sdNames.NAME eq username }.firstOrNull()
                if (existing == null) {
                    sdNames.insert {
                        it[NAME] = username
                        it[ID] = id
                    }
                    return@suspendTransaction SDInsertStatus.SUCCESS
                }
                val currentOwner = existing[sdNames.ID]
                if (currentOwner == id) {
                    SDInsertStatus.ALREADY_OWNED_BY_YOU
                } else {
                    notificationService.sendApprovalNotification(name, username, id, currentOwner)
                    SDInsertStatus.ALREADY_OWNED_BY_OTHER
                }
            }
        }
    }

    /**
     * Set the owner of the sd name to the given user, regardless of a potential former owner
     * @param username the sd name (already as username)
     * @param id the discord user id
     */
    override suspend fun setOwnerOfName(username: String, id: Long) {
        suspendTransaction(db) {
            sdNames.upsert {
                it[NAME] = username
                it[ID] = id
            }
        }
    }
}

@Single
class JDASDNamesNotificationService(
    val jda: JDA
) : SDNamesNotificationService {
    override suspend fun sendApprovalNotification(name: String, username: String, id: Long, currentOwner: Long) {
        jda.getTextChannelById(SDNAMES_CHANNEL_ID)!!.send(
            "<@$id> [$id] (`${
                jda.retrieveUserById(id).await().effectiveName
            }`) möchte den Namen `$name` [`$username`] haben, aber dieser ist bereits von " +
                    "<@$currentOwner> [$currentOwner] (`${
                        jda.retrieveUserById(currentOwner).await().effectiveName
                    }`) belegt! Akzeptieren?",
            components = listOf(
                SDNamesApprovalButton.withoutIData(label = K18n_Yes, buttonStyle = ButtonStyle.SUCCESS) {
                    accept = true; this.id = id; this.username = username
                },
                SDNamesApprovalButton.withoutIData(
                    label = K18n_No,
                    buttonStyle = ButtonStyle.DANGER
                ) { accept = false }
            ).into()
        ).queue()
    }
}

private const val SDNAMES_CHANNEL_ID = 1148173270726737961

enum class SDInsertStatus {
    SUCCESS, ALREADY_OWNED_BY_YOU, ALREADY_OWNED_BY_OTHER
}
