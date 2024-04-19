package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.features.flo.SDNamesApprovalButton
import de.tectoast.emolga.utils.toUsername
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object SDNamesDB : Table("sdnames") {
    val NAME = varchar("name", 18)
    val ID = long("id")

    suspend fun getIDByName(name: String) =
        newSuspendedTransaction { selectAll().where { NAME eq name.toUsername() }.firstOrNull()?.get(ID) } ?: -1

    fun addIfAbsent(name: String, id: Long): Deferred<SDInsertStatus> {
        return Database.dbScope.async {
            newSuspendedTransaction {
                val username = name.toUsername()
                val existing = selectAll().where { NAME eq username }.firstOrNull()
                if (existing == null) {
                    insert {
                        it[NAME] = username
                        it[ID] = id
                    }
                    return@newSuspendedTransaction SDInsertStatus.SUCCESS
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

    suspend fun replace(username: String, id: Long) {
        newSuspendedTransaction {
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
