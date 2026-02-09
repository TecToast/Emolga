package de.tectoast.emolga.features.flo

import com.google.common.reflect.ClassPath
import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.k18n
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.migration.MigrationUtils
import kotlin.reflect.full.isSubclassOf

object DBMigration {
    private val statementsById = mutableMapOf<String, List<String>>()

    object Button : ButtonFeature<Button.Args>(::Args, ButtonSpec("dbmigration")) {
        override val buttonStyle = ButtonStyle.PRIMARY
        override val label = "Execute".k18n

        class Args : Arguments() {
            var id: String by string()
        }


        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            dbTransaction {
                val statements = statementsById[e.id] ?: return@dbTransaction iData.reply(
                    "No statements found for id `${e.id}`",
                    ephemeral = true
                )
                val tx = TransactionManager.current()
                for (sql in statements) {
                    tx.exec(sql)
                }
                statementsById.remove(e.id)
            }
            iData.edit(contentK18n = null, components = Button(disabled = true) { this.id = "" }.into())
        }
    }

    object Command : CommandFeature<NoArgs>(NoArgs(), CommandSpec("dbmigration", "DB Migration".k18n)) {

        init {
            registerListener<ReadyEvent> {
                sendMigrationStatements(it.jda.getTextChannelById(447357526997073932)!!)
            }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.done(true)
            sendMigrationStatements(iData.messageChannel)
        }
    }

    private suspend fun sendMigrationStatements(channel: MessageChannel) {
        dbTransaction {
            val statements = MigrationUtils.statementsRequiredForDatabaseMigration(
                *ClassPath.from(Thread.currentThread().contextClassLoader)
                    .getTopLevelClassesRecursive("de.tectoast.emolga")
                    .map { it.load().kotlin }
                    .filter { it.isSubclassOf(Table::class) }.mapNotNull { it.objectInstance as? Table? }
                    .toTypedArray(), withLogs = false
            )
            if (statements.isEmpty()) return@dbTransaction
            val id = System.currentTimeMillis().toString()
            statementsById[id] = statements
            channel.send(
                statements.joinToString(separator = "\n", prefix = "```sql\n", postfix = "\n```") { "$it;" },
                components = Button.withoutIData {
                    this.id = id
                }.into()
            ).queue()
        }
    }
}
