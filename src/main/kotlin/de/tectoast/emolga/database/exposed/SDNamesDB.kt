package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.database.Database
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.danger
import dev.minn.jda.ktx.interactions.components.success
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object SDNamesDB : Table("sdnames") {
    val NAME = varchar("name", 18)
    val ID = long("id")

    fun getIDByName(name: String) =
        transaction { select { NAME eq Command.toUsername(name) }.firstOrNull()?.get(ID) } ?: -1

    fun addIfAbsent(name: String, id: Long): Deferred<SDInsertStatus> {
        return Database.dbScope.async {
            newSuspendedTransaction {
                val username = Command.toUsername(name)
                val existing = select { NAME eq username }.firstOrNull()
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
                    emolgajda.getTextChannelById(SDNAMES_CHANNEL_ID)!!.send(
                        "<@$id> [$id] (`${
                            emolgajda.retrieveUserById(id).await().effectiveName
                        }`) möchte den Namen `$name` [`$username`] haben, aber dieser ist bereits von " +
                                "<@$currentOwner> [$currentOwner] (`${
                                    emolgajda.retrieveUserById(currentOwner).await().effectiveName
                                }`) belegt! Akzeptieren?",
                        components = listOf(
                            success("sdnamesapproval;true;$id;$username", "Ja"),
                            danger("sdnamesapproval;false", "Nein")
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
