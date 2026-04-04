package de.tectoast.emolga.features.flo

import com.google.common.reflect.ClassPath
import de.tectoast.emolga.di.JDAReadyTask
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.k18n
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.migration.r2dbc.MigrationUtils
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.isSubclassOf

object DBMigration {

    @Single(binds = [ListenerProvider::class])
    class Button(val dbMigration: DatabaseMigrationService) :
        ButtonFeature<Button.Args>(::Args, ButtonSpec("dbmigration")) {
        override val buttonStyle = ButtonStyle.PRIMARY
        override val label = "Execute".k18n

        class Args : Arguments() {
            var id: String by string()
        }


        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            if (!dbMigration.executeMigration(e.id)) {
                return iData.reply("Migration failed, id not found.")
            }
            iData.edit(contentK18n = null, components = this(disabled = true) { this.id = "" }.into())
        }
    }

    @Single(binds = [ListenerProvider::class])
    class Command(val dbMigration: DatabaseMigrationService, val btn: Button) :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("dbmigration", "DB Migration".k18n)), JDAReadyTask {
        private val initialMigrationScope = createCoroutineScope("InitialMigration", Dispatchers.IO)

        override suspend fun onJDAReady(jda: JDA) {
            initialMigrationScope.launch {
                sendMigrationStatements(jda.getTextChannelById(447357526997073932)!!)
            }
        }


        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.done(true)
            sendMigrationStatements(iData.messageChannel)
        }

        private suspend fun sendMigrationStatements(channel: MessageChannel) {
            val (id, statements) = dbMigration.generateMigrationPlan() ?: return
            channel.send(
                statements.joinToString(separator = "\n", prefix = "```sql\n", postfix = "\n```") { "$it;" },
                components = btn.withoutIData {
                    this.id = id
                }.into()
            ).queue()
        }
    }

}

@Single
class DatabaseMigrationService(val db: R2dbcDatabase) {
    private val statementsById = ConcurrentHashMap<String, List<String>>()

    suspend fun generateMigrationPlan(): Pair<String, List<String>>? = suspendTransaction(db) {
        val statements =
            MigrationUtils.statementsRequiredForDatabaseMigration(
                *ClassPath.from(Thread.currentThread().contextClassLoader)
                    .getTopLevelClassesRecursive("de.tectoast.emolga").map { it.load().kotlin }
                    .filter { it.isSubclassOf(Table::class) }.mapNotNull { it.objectInstance as? Table? }
                    .toTypedArray(), withLogs = false)
        if (statements.isEmpty()) return@suspendTransaction null
        val id = System.currentTimeMillis().toString()
        statementsById[id] = statements
        id to statements
    }

    suspend fun executeMigration(id: String) = suspendTransaction(db) {
        val statements = statementsById[id] ?: return@suspendTransaction false
        for (sql in statements) {
            this.exec(sql)
        }
        statementsById.remove(id)
        true
    }

}