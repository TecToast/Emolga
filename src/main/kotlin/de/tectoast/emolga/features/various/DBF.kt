package de.tectoast.emolga.features.various

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.DumbestFliesDB
import de.tectoast.emolga.database.exposed.UsedQuestionsDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.various.DBF.Button
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.condAppend
import de.tectoast.emolga.utils.file
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.generics.getChannel
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import dev.minn.jda.ktx.util.ref
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.ActionRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SimpleLifeBarEvent(
    val maxLifes: Int,
    val hasVote: Boolean,
    val hasAnswer: Boolean,
    val jda: JDA,
    val host: Long,
    gameChannelId: Long,
    users: List<Member>,
) {
    val members = mutableMapOf<Long, String>()
    var adminStatusID = -1L
    var gameStatusID = -1L
    val lifes = mutableMapOf<Long, Int>().withDefault { maxLifes }
    val votes = mutableMapOf<Long, Long>()
    val answers = mutableMapOf<Long, String>()
    var votedSet: List<Map.Entry<Long, Int>> = listOf()

    val gameChannel by jda.getTextChannelById(gameChannelId)!!.ref()
    val adminChannel by jda.openPrivateChannelById(host).complete().ref()
    private val gameStatusComponents
        get() = buildList {
            if (hasAnswer) {
                add(
                    ActionRow.of(
                        SimpleLifeBarEventManager.AnswerButton()
                    )
                )
            }
            if (hasVote) add(
                ActionRow.of(
                    SimpleLifeBarEventManager.VoteMenu(
                        "Wähle eine Person c:",
                        options = members.entries.map { SelectOption(it.value, it.key.toString()) }) {
                        this.host = host
                    })
            )
        }

    private val adminStatusComponents
        get() = buildList {

        }

    init {
        users.forEach {
            members[it.idLong] = it.effectiveName
            lifes[it.idLong] = maxLifes
        }
        adminStatusID = adminChannel.send(
            buildString {
                if (hasAnswer) append("Antworten:\n\n")
                if (hasVote) append("Votes:\n\nStand der Dinge:")
            }, components = Button("Neue Runde") { mode = Button.Mode.NEWROUND }.into()
        ).complete().idLong
        gameStatusID = gameChannel.send(
            generateGameStatusMessage(), components = if (hasVote) gameStatusComponents else emptyList()
        ).complete().idLong
    }

    fun loseLive(id: Long) {
        lifes[id] = (lifes.getValue(id) - 1).coerceAtLeast(0)
        if (lifes[id] == 0) {
            members.remove(id)
            lifes.remove(id)
        }
    }

    private fun allVoted() = votes.size == members.size

    private fun generateGameStatusMessage() = "Leben:\n${
        members.keys.joinToString("\n") {
            val currentLifes = lifes.getValue(it)
            "<@${it}>: ${"<:heartfull:1126238135664255036>".repeat(currentLifes)}${
                "<:heartempty:1126238132619202610>".repeat(
                    maxLifes - currentLifes
                )
            }".condAppend(it in votes, " ✅").condAppend(it in answers, " ✉")
        }
    }"


    suspend fun updateAdminStatusMessage() {
        adminChannel.editMessageById(
            adminStatusID, buildString {
                if (hasAnswer) {
                    append("Antworten:\n")
                    append(answers.entries.joinToString("\n") { "<@${it.key}>: `${it.value}`" })
                }
                if (hasVote) {
                    val entries =
                        DBF.votes.entries.groupingBy { it.value }.eachCount().entries.sortedByDescending { it.value }
                    if (allVoted()) votedSet = entries
                    append(
                        "Votes:\n${
                            DBF.votes.entries.joinToString("\n") {
                                "<@${it.key}> -> <@${it.value}>"
                            }
                        }\n\nStand der Dinge:\n${
                            entries.joinToString("\n") { "<@${it.key}>: ${it.value}" }
                        }")
                }

            }).await()
    }

    private suspend fun updateGameStatusMessage() {
        gameStatusID.takeIf { it != -1L }?.let {
            gameChannel.editMessage(
                it.toString(), generateGameStatusMessage(), components = gameStatusComponents
            ).await()
        }
    }

    context(InteractionData)
    suspend fun addVote(from: Long, to: Long) {
        if (!hasVote) return reply("Es gibt keine Votes!", ephemeral = true)
        if (from !in members) return reply("Du spielst nicht mit!", ephemeral = true)
        if (from == to) return reply("Du kannst nicht für dich selbst voten!")
        votes[from] = to
        updateGameStatusMessage()
        updateAdminStatusMessage()
        if (allVoted()) adminChannel.sendMessage("Alle haben gevotet!").delay(1.seconds.toJavaDuration()).flatMap(
            Message::delete
        ).queue()
    }

    context(InteractionData)
    suspend fun addAnswer(from: Long, input: String) {
        if (!hasAnswer) return reply("Es gibt keine Inputs!", ephemeral = true)
        if (from !in members) return reply("Du spielst nicht mit!", ephemeral = true)
        answers[from] = input
        updateGameStatusMessage()
        updateAdminStatusMessage()
        if (answers.size == members.size) adminChannel.sendMessage("Alle haben geantwortet!")
            .delay(1.seconds.toJavaDuration()).flatMap(
                Message::delete
            ).queue()
    }

    context(InteractionData)
    suspend fun removeLifes(users: List<Long>) {
        users.forEach {
            loseLive(it)
        }
        updateGameStatusMessage()
        updateAdminStatusMessage()
    }

    context(InteractionData)
    fun replyWithAnswerModal() {
        replyModal(SimpleLifeBarEventManager.AnswerModal {
            this.host = host
        })
    }
}

object SimpleLifeBarEventManager {
    val events = mutableMapOf<Long, SimpleLifeBarEvent>()

    object Command :
        CommandFeature<Command.Args>(::Args, CommandSpec("simplelifebar", "simplelifebar", Constants.G.COMMUNITY)) {

        class Args : Arguments() {
            var maxlifes by int("maxlifes", "maxlifes") {
                default = 3
            }
            var hasVote by boolean(
                "hasVote", "Ob die Teilmehmer abstimmen können sollen, wer ein Leben verliert (Der Dümmste fliegt)"
            )
            var hasInput by boolean(
                "hasInput", "Ob die Teilnehmer eine Eingabe machen können sollen, die der Moderator sehen kann"
            )
            var users by genericList<Member, Member>("user", "Teilnehmer", 10, 1, OptionType.USER)
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            events[user] = SimpleLifeBarEvent(
                maxLifes = e.maxlifes,
                hasVote = e.hasVote,
                hasAnswer = e.hasInput,
                jda = jda,
                host = user,
                gameChannelId = tc,
                users = e.users
            )
        }


    }

    object VoteMenu : SelectMenuFeature<VoteMenu.Args>(::Args, SelectMenuSpec("simplelifebar")) {
        class Args : Arguments() {
            var host by long().compIdOnly()
            var user by singleOption { it.toLong() }
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val event = events[e.host] ?: return reply("Das Event läuft derzeit nicht!", ephemeral = true)
            event.addVote(user, e.user)
        }
    }

    object AnswerButton : ButtonFeature<AnswerButton.Args>(::Args, ButtonSpec("simplelifebar")) {
        override val label = "Antwort abgeben"

        class Args : Arguments() {
            var host by long().compIdOnly()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val event = events[e.host] ?: return reply("Das Event läuft derzeit nicht!", ephemeral = true)
            event.replyWithAnswerModal()
        }
    }

    object AnswerModal : ModalFeature<AnswerModal.Args>(::Args, ModalSpec("simplelifebar")) {
        override val title = "Antwort eingeben"

        class Args : Arguments() {
            var host by long().compIdOnly()
            var answer by string("Antwort", "Deine Antwort")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val event = events[e.host] ?: return reply("Das Event läuft derzeit nicht!", ephemeral = true)
            event.addAnswer(user, e.answer)
        }
    }

    object LifeDecreaseMenu : SelectMenuFeature<LifeDecreaseMenu.Args>(::Args, SelectMenuSpec("simplelifebar")) {
        class Args : Arguments() {
            var host by long().compIdOnly()
            var users by multiOption(IntRange.EMPTY) { it.toLong() }
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val event = events[e.host] ?: return reply("Das Event läuft derzeit nicht!", ephemeral = true)
            event.removeLifes(e.users)
        }
    }
}

object DBF {
    val members = mutableMapOf<Long, String>()
    val votes = mutableMapOf<Long, Long>()
    var adminStatusID = -1L
    var gameStatusID = -1L
    var maxLifes = 3
    val lifes = mutableMapOf<Long, Int>().withDefault { maxLifes }
    var votedSet: List<Map.Entry<Long, Int>> = listOf()

    private val logger = KotlinLogging.logger {}

    object Button : ButtonFeature<Button.Args>(::Args, ButtonSpec("dumbestflies")) {
        class Args : Arguments() {
            var mode by enumBasic<Mode>()
        }

        enum class Mode {
            NEWROUND, QUESTION, ESTIMATE
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            when (e.mode) {
                Mode.NEWROUND -> endOfRound()
                Mode.QUESTION -> newNormalQuestion()
                Mode.ESTIMATE -> newEstimateQuestion()
            }
        }
    }

    object Menu : SelectMenuFeature<Menu.Args>(::Args, SelectMenuSpec("dumbestflies")) {
        class Args : Arguments() {
            var user by singleOption { it.toLong() }
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            addVote(e.user)
        }
    }

    object Command :
        CommandFeature<NoArgs>(NoArgs(), CommandSpec("dumbestfliesctl", "dumbestfliesctl", Constants.G.COMMUNITY)) {

        init {
            restrict(henny)
            slashPrivate()
        }

        class UserArg : Arguments() {
            var user by member("user", "user")
        }


        object Add : CommandFeature<UserArg>(::UserArg, CommandSpec("add", "add")) {

            context(InteractionData)
            override suspend fun exec(e: UserArg) {
                runCatching {
                    newSuspendedTransaction {
                        DumbestFliesDB.insert {
                            it[id] = e.user.idLong
                            it[name] = e.user.effectiveName
                        }
                    }
                }
                reloadMembers()
                done(true)
            }
        }

        object Remove : CommandFeature<UserArg>(::UserArg, CommandSpec("remove", "remove")) {
            context(InteractionData)
            override suspend fun exec(e: UserArg) {
                newSuspendedTransaction {
                    DumbestFliesDB.deleteWhere { id eq e.user.idLong }
                }
                reloadMembers()
                done(true)
            }
        }

        object List : CommandFeature<NoArgs>(NoArgs(), CommandSpec("list", "list")) {
            context(InteractionData)
            override suspend fun exec(e: NoArgs) {
                reply(newSuspendedTransaction {
                    DumbestFliesDB.selectAll()
                        .joinToString("\n") { it[DumbestFliesDB.name] + " (<@${it[DumbestFliesDB.id]}>)" }
                }, ephemeral = true)
            }
        }

        object Start : CommandFeature<Start.Args>(::Args, CommandSpec("start", "start")) {
            class Args : Arguments() {
                var maxlifes by int("maxlifes", "maxlifes") {
                    default = 3
                }
            }

            context(InteractionData)
            override suspend fun exec(e: Args) {
                initWithDB(e.maxlifes)
                done(true)
            }
        }

        object Tie : CommandFeature<UserArg>(::UserArg, CommandSpec("tie", "tie")) {
            context(InteractionData)
            override suspend fun exec(e: UserArg) {
                realEndOfRound(e.user.idLong)
            }
        }

        object StartQuestions : CommandFeature<NoArgs>(NoArgs(), CommandSpec("startquestions", "startquestions")) {
            context(InteractionData)
            override suspend fun exec(e: NoArgs) {
                startQuestions()
            }
        }

        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            // do nothing
        }
    }

    suspend fun initWithDB(lifesAmount: Int) = newSuspendedTransaction {
        maxLifes = lifesAmount
        votes.clear()
        reloadMembers()
        logger.info(members.toString())
        logger.info(lifes.toString())
        adminStatusID = adminChannel().send(
            "Votes:", components = Button("Neue Runde") { mode = Button.Mode.NEWROUND }.into()
        ).await().idLong
        gameStatusID = gameChannel.send(
            generateGameStatusMessage(), components = playerSelectMenu
        ).await().idLong
    }

    suspend fun reloadMembers() {
        members.clear()
        newSuspendedTransaction {
            DumbestFliesDB.selectAll().forEach {
                val id = it[DumbestFliesDB.id]
                members[id] = it[DumbestFliesDB.name]
                lifes[id] = maxLifes
            }
        }
        updateGameStatusMessage()
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

    context(InteractionData)
    suspend fun addVote(to: Long) {
        val from = user
        if (from !in members) return
        if (from == to) return reply("Du kannst nicht für dich selbst voten!", ephemeral = true)
        //if (votes.containsKey(from)) return e.reply_("Du hast bereits diese Runde gevotet!", ephemeral = true).queue()
        votes[from] = to
        reply("Du hast für <@$to> gevotet!", ephemeral = true)
        updateGameStatusMessage()
        updateAdminStatusMessage()
        if (allVoted()) adminChannel().sendMessage("Alle haben gevotet! <@${Constants.FLOID}>")
            .delay(1.seconds.toJavaDuration()).flatMap(
                Message::delete
            ).queue()
    }

    suspend fun updateAdminStatusMessage() {
        val entries = votes.entries.groupingBy { it.value }.eachCount().entries.sortedByDescending { it.value }
        if (allVoted()) votedSet = entries
        adminChannel().editMessageById(
            adminStatusID, "Votes:\n${
            votes.entries.joinToString("\n") {
                "<@${it.key}> -> <@${it.value}>"
            }
        }\n\nStand der Dinge:\n${
            entries.joinToString("\n") { "<@${it.key}>: ${it.value}" }
        }").await()
    }

    private suspend fun updateGameStatusMessage() {
        gameStatusID.takeIf { it != -1L }?.let {
            gameChannel.editMessage(it.toString(), generateGameStatusMessage(), components = playerSelectMenu).await()
        }
    }

    context(InteractionData)
    suspend fun endOfRound() {
        if (!allVoted()) return reply("Es haben noch nicht alle gevoted!", ephemeral = true)
        val highestVote = votedSet.maxOf { it.value }
        val members = votedSet.filter { it.value == highestVote }.map { it.key }
        if (members.size > 1) {
            return reply(
                "Es gibt einen Gleichstand zwischen: ${
                members.joinToString("") { "<@${it}>" }
            }", ephemeral = true)
        }
        realEndOfRound(members.first())
    }

    fun loseLive(id: Long) {
        lifes[id] = (lifes.getValue(id) - 1).coerceAtLeast(0)
        if (lifes[id] == 0) {
            members.remove(id)
            lifes.remove(id)
        }
    }

    context(InteractionData)
    suspend fun realEndOfRound(loser: Long) {
        loseLive(loser)
        votes.clear()
        updateAdminStatusMessage()
        updateGameStatusMessage()
        reply("Neue Runde gestartet!", ephemeral = true)
    }

    @Suppress("unused")
    private sealed class AdminChannelProvider {
        abstract suspend fun provideChannel(): MessageChannel

        data object MyServer : AdminChannelProvider() {
            override suspend fun provideChannel(): MessageChannel =
                jda.getChannel<MessageChannel>(1126196072839139490)!!
        }

        class PN(private val uid: Long) : AdminChannelProvider() {
            override suspend fun provideChannel(): MessageChannel = jda.openPrivateChannelById(uid).await()
        }
    }

    private val adminChannelProvider = AdminChannelProvider.PN(Constants.M.HENNY)
    private val gameChannel get() = jda.getChannel<GuildMessageChannel>(1314561631933562922)!!
    private suspend fun adminChannel() = adminChannelProvider.provideChannel()
    private fun allVoted() = votes.size == members.size
    lateinit var allQuestions: List<String>
    lateinit var regularQuestions: MutableSet<String>
    lateinit var estimateQuestions: MutableSet<String>
    val questionComponents = listOf(
        Button("Normal") { mode = Button.Mode.QUESTION },
        Button("Schätzen") { mode = Button.Mode.ESTIMATE },
    ).into()

    private fun readQuestions(): List<String> {
        val questions = mutableSetOf<String>()
        val streak = mutableMapOf<String, String>()
        for (line in "questionsraw.txt".file().readLines()) {
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
        return questions.toList()
    }

    @Suppress("unused")
    suspend fun sendQuestionsToUser() {
        val questions = readQuestions().toMutableList()
        val indexesToRemove = newSuspendedTransaction { UsedQuestionsDB.selectAll().map { it[UsedQuestionsDB.index] } }
        // remove from questions all indexes in indexesToRemove
        for (i in indexesToRemove.sortedDescending()) {
            questions.removeAt(i)
        }
        File("questionsthatarenotused.txt").writeText(questions.joinToString("\n"))
    }

    private fun allQuestionsWith(estimate: Boolean) =
        allQuestions.filter { it.startsWith("Schätzfrage") == estimate }.toMutableSet()

    context(InteractionData)
    fun startQuestions() {
        allQuestions = readQuestions()
        regularQuestions = allQuestionsWith(false)
        estimateQuestions = allQuestionsWith(true)
        reply("Fragen gestartet!", components = questionComponents)
    }

    context(InteractionData)
    suspend fun newNormalQuestion() {
        val question = regularQuestions.randomOrNull() ?: return reply(
            "Keine Fragen mehr!", ephemeral = true
        )
        regularQuestions.remove(question)
        reply("[${regularQuestions.size}] $question", components = questionComponents, ephemeral = true)
        UsedQuestionsDB.insertIndex(allQuestions.indexOf(question))
    }

    context(InteractionData)
    suspend fun newEstimateQuestion() {
        val question = estimateQuestions.randomOrNull() ?: return reply(
            "Keine Fragen mehr!", ephemeral = true
        )
        estimateQuestions.remove(question)
        reply("[${estimateQuestions.size}] $question", components = questionComponents, ephemeral = true)
        UsedQuestionsDB.insertIndex(allQuestions.indexOf(question))
    }

}
