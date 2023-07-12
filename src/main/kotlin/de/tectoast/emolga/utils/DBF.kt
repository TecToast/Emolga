package de.tectoast.emolga.utils

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.commands.file
import de.tectoast.emolga.commands.randomWithCondition
import de.tectoast.emolga.database.exposed.DumbestFliesDB
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.generics.getChannel
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object DBF {
    val members = mutableMapOf<Long, String>()
    val votes = mutableMapOf<Long, Long>()
    var adminStatusID = -1L
    var gameStatusID = -1L
    var maxLifes = 3
    val lifes = mutableMapOf<Long, Int>().withDefault { maxLifes }
    var votedSet: List<Map.Entry<Long, Int>> = listOf()

    private val logger = KotlinLogging.logger {}
    suspend fun initWithDB(lifesAmount: Int) = newSuspendedTransaction {
        maxLifes = lifesAmount
        members.clear()
        votes.clear()
        DumbestFliesDB.selectAll().forEach {
            val id = it[DumbestFliesDB.id]
            members[id] = it[DumbestFliesDB.name]
            lifes[id] = lifesAmount
        }
        logger.info(members.toString())
        logger.info(lifes.toString())
        adminStatusID = adminChannel.send(
            "Votes:", components = listOf(
                primary("dumbestflies;newround", "Neue Runde"),
            ).into()
        ).await().idLong
        gameStatusID = gameChannel.send(
            generateGameStatusMessage(),
            components = playerSelectMenu
        ).await().idLong
    }

    private val playerSelectMenu
        get() = StringSelectMenu(
            "dumbestflies",
            "Wähle den Dümmsten c:",
            options = members.entries.map { SelectOption(it.value, it.key.toString()) }).into()

    private fun generateGameStatusMessage() = "Leben:\n${
        members.keys.joinToString("\n") {
            "<@${it}>: ${"<:heartfull:1126238135664255036>".repeat(lifes.getValue(it))}${
                "<:heartempty:1126238132619202610>".repeat(
                    maxLifes - lifes.getValue(it)
                )
            }".condAppend(
                it in votes, " ✅"
            )
        }
    }"

    suspend fun addVote(e: StringSelectInteractionEvent) {
        val from = e.user.idLong
        val to = e.values.first().toLong()
        if (from !in members) return
        if (from == to) return e.reply_("Du kannst nicht für dich selbst voten!", ephemeral = true).queue()
        //if (votes.containsKey(from)) return e.reply_("Du hast bereits diese Runde gevotet!", ephemeral = true).queue()
        votes[from] = to
        e.reply_("Du hast für <@$to> gevotet!", ephemeral = true).queue()
        updateGameStatusMessage()
        updateAdminStatusMessage()
        if (allVoted()) adminChannel.sendMessage("Alle haben gevotet! <@${Constants.FLOID}>")
            .delay(1.seconds.toJavaDuration()).flatMap(
                Message::delete
            ).queue()
    }

    suspend fun updateAdminStatusMessage() {
        val entries = votes.entries.groupingBy { it.value }.eachCount().entries.sortedByDescending { it.value }
        if (allVoted()) votedSet = entries
        adminChannel.editMessageById(adminStatusID, "Votes:\n${
            votes.entries.joinToString("\n") {
                "<@${it.key}> -> <@${it.value}>"
            }
        }\n\nStand der Dinge:\n${
            entries.joinToString("\n") { "<@${it.key}>: ${it.value}" }
        }"
        ).await()
    }

    private suspend fun updateGameStatusMessage() {
        gameChannel.editMessage(gameStatusID.toString(), generateGameStatusMessage(), components = playerSelectMenu)
            .await()
    }

    suspend fun endOfRound(e: ButtonInteractionEvent) {
        if (!allVoted()) return e.reply_("Es haben noch nicht alle gevoted!", ephemeral = true).queue()
        val highestVote = votedSet.maxOf { it.value }
        val members = votedSet.filter { it.value == highestVote }.map { it.key }
        if (members.size > 1) {
            e.reply_("Es gibt einen Gleichstand zwischen: ${
                members.joinToString("") { "<@${it}>" }
            }", ephemeral = true).queue()
            return
        }
        realEndOfRound(e, members.first())
    }

    fun loseLive(id: Long) {
        lifes[id] = (lifes.getValue(id) - 1).coerceAtLeast(0)
        if (lifes[id] == 0) {
            members.remove(id)
            lifes.remove(id)
        }
    }

    suspend fun realEndOfRound(e: IReplyCallback, loser: Long) {
        loseLive(loser)
        votes.clear()
        updateAdminStatusMessage()
        updateGameStatusMessage()
        e.reply_("Neue Runde gestartet!", ephemeral = true).queue()
    }

    private val gameChannel get() = EmolgaMain.emolgajda.getChannel<GuildMessageChannel>(1126193988051931277)!!
    private val adminChannel get() = EmolgaMain.emolgajda.getChannel<GuildMessageChannel>(1126196072839139490)!!
    private fun allVoted() = votes.size == members.size
    lateinit var allQuestions: MutableSet<String>
    val questionComponents = listOf(
        primary("dumbestflies;question:normal", "Normal"),
        primary("dumbestflies;question:estimate", "Schätzen"),
    ).into()

    private fun readQuestions(): MutableSet<String> {
        val questions = mutableSetOf<String>()
        val streak = mutableMapOf<String, String>()
        for (line in "questionsraw.txt".file().readLines()) {
            //if("?" !in line) println(line)
            if (streak.isNotEmpty() && !line.endsWith(">")) {
                questions += streak.entries.joinToString("\n") { it.key + " ---> " + it.value }
                streak.clear()
            }
            val question =
                if ("?." in line) line.substringBeforeLast("?.") + "." else line.substringBeforeLast("?") + "?"
            val answer =
                line.substringAfterLast("?").trim().let { if (it.startsWith(".")) it.substring(1) else it }.trim()
            if (line.endsWith(">")) {
                streak[question] = answer.substringBeforeLast(">")
                continue
            }
            questions += "$question ---> $answer"

        }
        return questions
    }

    fun startQuestions(e: SlashCommandInteractionEvent) {
        allQuestions = readQuestions()
        e.reply_("Fragen gestartet!", components = questionComponents).queue()
    }

    fun newNormalQuestion(e: ButtonInteractionEvent) {
        val question = allQuestions.randomWithCondition { !it.startsWith("Schätzfrage") } ?: return e.reply_(
            "Keine Fragen mehr!",
            ephemeral = true
        ).queue()
        allQuestions.remove(question)
        e.reply_("[${allQuestions.size}] $question", components = questionComponents, ephemeral = true).queue()
    }

    fun newEstimateQuestion(e: ButtonInteractionEvent) {
        val question = allQuestions.randomWithCondition { it.startsWith("Schätzfrage") } ?: return e.reply_(
            "Keine Fragen mehr!",
            ephemeral = true
        ).queue()
        allQuestions.remove(question)
        e.reply_("[${allQuestions.size}] $question", components = questionComponents, ephemeral = true).queue()
    }

}
