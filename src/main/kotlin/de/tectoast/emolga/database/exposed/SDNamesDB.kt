package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.features.flo.SDNamesApprovalButton
import de.tectoast.emolga.utils.toUsername
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

object SDNamesDB : Table("sdnames") {
    val NAME = varchar("name", 18)
    val ID = long("id")

    override val primaryKey = PrimaryKey(NAME)

    /**
     * Get a discord user id by the showdown name
     * @param name the showdown name to look for
     * @return the corresponding user id, or -1 if no user could be found
     */
    suspend fun getIDByName(name: String) =
        dbTransaction { selectAll().where { NAME eq name.toUsername() }.firstOrNull()?.get(ID) } ?: -1

    /**
     * Adds a showdown name/id combination to the database (if the name is not already used)
     * @param name the showdown name
     * @param id the discord user id
     * @return a Deferred resolvig to a [SDInsertStatus] showing the result of the operation
     */
    fun addIfAbsent(name: String, id: Long): Deferred<SDInsertStatus> {
        return Database.dbScope.async {
            dbTransaction {
                val username = name.toUsername()
                val existing = selectAll().where { NAME eq username }.firstOrNull()
                if (existing == null) {
                    insert {
                        it[NAME] = username
                        it[ID] = id
                    }
                    return@dbTransaction SDInsertStatus.SUCCESS
                }
                val currentOwner = existing[ID]
                if (currentOwner == id) {
                    SDInsertStatus.ALREADY_OWNED_BY_YOU
                } else {
                    jda.getTextChannelById(SDNAMES_CHANNEL_ID)!!.send(
                        "<@$id> [$id] (`${
                            jda.retrieveUserById(id).await().effectiveName
                        }`) möchte den Namen `$name` [`$username`] haben, aber dieser ist bereits von " +
                                "<@$currentOwner> [$currentOwner] (`${
                                    jda.retrieveUserById(currentOwner).await().effectiveName
                                }`) belegt! Akzeptieren?",
                        components = listOf(
                            SDNamesApprovalButton("Ja", ButtonStyle.SUCCESS) {
                                accept = true; this.id = id; this.username = username
                            },
                            SDNamesApprovalButton("Nein", ButtonStyle.DANGER) { accept = false }
                        ).into()
                    ).queue()
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
    suspend fun setOwnerOfName(username: String, id: Long) {
        dbTransaction {
            deleteWhere { NAME eq username }
            insert {
                it[NAME] = username
                it[ID] = id
            }
        }
    }
}

private const val SDNAMES_CHANNEL_ID = 1148173270726737961

enum class SDInsertStatus {
    SUCCESS, ALREADY_OWNED_BY_YOU, ALREADY_OWNED_BY_OTHER
}
