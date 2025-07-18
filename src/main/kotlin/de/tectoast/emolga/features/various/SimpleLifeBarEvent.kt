package de.tectoast.emolga.features.various

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.various.SimpleLifeBarEventManager.AdminResetButton.ResetType
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.condAppend
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.MessageEdit
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.send
import dev.minn.jda.ktx.util.ref
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.ActionRow
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SimpleLifeBarEvent(
    val maxLifes: Int,
    val hasVote: Boolean,
    val hasAnswer: Boolean,
    val jda: JDA,
    val gameHost: Long,
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
    val adminChannel by jda.openPrivateChannelById(gameHost).complete().ref()
    private val membersAsSelectOptions get() = members.entries.map { SelectOption(it.value, it.key.toString()) }
    private val gameStatusComponents
        get() = buildList {
            if (hasAnswer) {
                add(
                    ActionRow.of(
                        SimpleLifeBarEventManager.AnswerButton {
                            this.host = gameHost
                        }
                    )
                )
            }
            if (hasVote) add(
                ActionRow.of(
                    SimpleLifeBarEventManager.VoteMenu(
                        "Wähle eine Person c:",
                        options = membersAsSelectOptions
                    ) {
                        this.host = gameHost
                    })
            )
        }

    private val adminStatusComponents
        get() = buildList {
            if (hasVote) add(
                ActionRow.of(SimpleLifeBarEventManager.AcceptVotesButton {
                    this.host = gameHost
                })
            )
            add(
                ActionRow.of(
                    SimpleLifeBarEventManager.LifeDecreaseMenu(
                        "Wähle die Personen, die ein Leben verlieren sollen",
                        options = membersAsSelectOptions
                    ) {
                        this.host = gameHost
                    })
            )
            val resetButtons = buildList {
                if (hasVote) add(SimpleLifeBarEventManager.AdminResetButton("Reset Votes") {
                    this.host = gameHost
                    this.type = ResetType.VOTES
                })
                if (hasAnswer) add(SimpleLifeBarEventManager.AdminResetButton("Reset Answers") {
                    this.host = gameHost
                    this.type = ResetType.ANSWERS
                })
            }
            if (resetButtons.isNotEmpty()) add(ActionRow.of(resetButtons))
        }

    init {
        users.forEach {
            members[it.idLong] = it.effectiveName
            lifes[it.idLong] = maxLifes
        }
        adminStatusID = adminChannel.send(
            generateAdminStatusMessage(), components = adminStatusComponents
        ).complete().idLong
        gameStatusID = gameChannel.send(
            generateGameStatusMessage(), components = gameStatusComponents
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
            }".condAppend(it in votes, " ✅").condAppend(it in answers, " 📝")
        }
    }"


    suspend fun updateAdminStatusMessage() {
        adminChannel.editMessageById(
            adminStatusID, MessageEdit(content = generateAdminStatusMessage(), components = adminStatusComponents)
        ).await()
    }

    private fun generateAdminStatusMessage(): String = buildString {
        if (hasAnswer) {
            append("Antworten:\n")
            append(answers.entries.joinToString("\n") { "<@${it.key}>: `${it.value}`" })
        }
        if (hasVote) {
            val entries =
                votes.entries.groupingBy { it.value }.eachCount().entries.sortedByDescending { it.value }
            if (allVoted()) votedSet = entries
            append(
                "Votes:\n${
                    votes.entries.joinToString("\n") {
                        "<@${it.key}> -> <@${it.value}>"
                    }
                }\n\nStand der Dinge:\n${
                    entries.joinToString("\n") { "<@${it.key}>: ${it.value}" }
                }")
        }

    }

    private suspend fun updateGameStatusMessage() {
        gameStatusID.takeIf { it != -1L }?.let {
            gameChannel.editMessage(
                it.toString(), generateGameStatusMessage(), components = gameStatusComponents
            ).await()
        }
    }

    context(iData: InteractionData)
    suspend fun addVote(from: Long, to: Long) {
        if (!hasVote) return iData.reply("Es gibt keine Votes!", ephemeral = true)
        if (from !in members) return iData.reply("Du warst zu dumm!", ephemeral = true)
        if (from == to) return iData.reply("Nein, du kannst nicht für dich selbst voten!", ephemeral = true)
        iData.reply("Vote gesetzt!", ephemeral = true)
        votes[from] = to
        updateGameStatusMessage()
        updateAdminStatusMessage()
        if (allVoted()) adminChannel.sendMessage("Alle haben gevotet!").delay(1.seconds.toJavaDuration()).flatMap(
            Message::delete
        ).queue()
    }

    context(iData: InteractionData)
    suspend fun addAnswer(from: Long, input: String) {
        if (!hasAnswer) return iData.reply("Es gibt keine Inputs!", ephemeral = true)
        if (from !in members) return iData.reply("Du spielst nicht mit!", ephemeral = true)
        iData.reply("Antwort gesetzt!", ephemeral = true)
        answers[from] = input
        updateGameStatusMessage()
        updateAdminStatusMessage()
    }

    suspend fun removeLifes(users: List<Long>) {
        users.forEach {
            loseLive(it)
        }
        votes.clear()
        answers.clear()
        updateGameStatusMessage()
        updateAdminStatusMessage()
    }

    context(iData: InteractionData)
    fun replyWithAnswerModal() {
        iData.replyModal(SimpleLifeBarEventManager.AnswerModal {
            this.host = gameHost
        })
    }

    context(iData: InteractionData)
    suspend fun acceptVotes() {
        if (!allVoted()) return iData.reply("Es haben noch nicht alle gevotet!", ephemeral = true)
        val highestVote = votedSet.maxOf { it.value }
        val members = votedSet.filter { it.value == highestVote }.map { it.key }
        if (members.size > 1) {
            return iData.reply(
                "Es gibt einen Gleichstand zwischen: ${
                    members.joinToString("") { "<@${it}>" }
                }", ephemeral = true)
        }
        removeLifes(members.take(1))
        iData.reply("Leben entfernt!", ephemeral = true)
    }

    context(iData: InteractionData)
    suspend fun reset(type: ResetType) {
        when (type) {
            ResetType.VOTES -> votes.clear()
            ResetType.ANSWERS -> answers.clear()
        }
        updateAdminStatusMessage()
        updateGameStatusMessage()
        iData.deferEdit()
    }
}

object SimpleLifeBarEventManager {
    val events = mutableMapOf<Long, SimpleLifeBarEvent>()

    object Command :
        CommandFeature<Command.Args>(::Args, CommandSpec("simplelifebar", "simplelifebar", Constants.G.COMMUNITY)) {

        class Args : Arguments() {
            var hasVote by boolean(
                "hasVote", "Ob die Teilmehmer abstimmen können sollen, wer ein Leben verliert (Der Dümmste fliegt)"
            )
            var hasInput by boolean(
                "hasInput", "Ob die Teilnehmer eine Text-Eingabe machen können sollen, die der Moderator sehen kann"
            )
            var startLifes by int("Startleben", "Wie viele Leben die Teilnehmer zu Beginn haben sollen")
            var users by genericList<Member, Member>("user", "Teilnehmer", 10, 1, OptionType.USER)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            events[iData.user] = SimpleLifeBarEvent(
                maxLifes = e.startLifes,
                hasVote = e.hasVote,
                hasAnswer = e.hasInput,
                jda = jda,
                gameHost = iData.user,
                gameChannelId = iData.tc,
                users = e.users
            )
            iData.done(true)
        }


    }

    object VoteMenu : SelectMenuFeature<VoteMenu.Args>(::Args, SelectMenuSpec("simplelifebarvote")) {
        class Args : Arguments() {
            var host by long().compIdOnly()
            var user by singleOption { it.toLong() }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val event = events[e.host] ?: return iData.reply("Das Event läuft derzeit nicht!", ephemeral = true)
            event.addVote(iData.user, e.user)
        }
    }

    object AnswerButton : ButtonFeature<AnswerButton.Args>(::Args, ButtonSpec("simplelifebaranswer")) {
        override val label = "Antwort abgeben"

        class Args : Arguments() {
            var host by long().compIdOnly()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val event = events[e.host] ?: return iData.reply("Das Event läuft derzeit nicht!", ephemeral = true)
            event.replyWithAnswerModal()
        }
    }

    object AnswerModal : ModalFeature<AnswerModal.Args>(::Args, ModalSpec("simplelifebarmodal")) {
        override val title = "Antwort eingeben"

        class Args : Arguments() {
            var host by long().compIdOnly()
            var answer by string("Antwort", "Deine Antwort")
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val event = events[e.host] ?: return iData.reply("Das Event läuft derzeit nicht!", ephemeral = true)
            event.addAnswer(iData.user, e.answer)
        }
    }

    object LifeDecreaseMenu :
        SelectMenuFeature<LifeDecreaseMenu.Args>(::Args, SelectMenuSpec("simplelifebardecrease")) {

        class Args : Arguments() {
            var host by long().compIdOnly()
            var users by multiOption(IntRange.EMPTY) { it.toLong() }
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val event = events[e.host] ?: return iData.reply("Das Event läuft derzeit nicht!", ephemeral = true)
            event.removeLifes(e.users)
            iData.done(true)
        }
    }

    object AcceptVotesButton : ButtonFeature<AcceptVotesButton.Args>(::Args, ButtonSpec("simplelifebaracceptvotes")) {
        override val label = "Votes akzeptieren / Neue Runde"

        class Args : Arguments() {
            var host by long().compIdOnly()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val event = events[e.host] ?: return iData.reply("Das Event läuft derzeit nicht!", ephemeral = true)
            event.acceptVotes()
        }
    }

    object AdminResetButton : ButtonFeature<AdminResetButton.Args>(::Args, ButtonSpec("simplelifebarreset")) {
        enum class ResetType {
            VOTES, ANSWERS
        }

        class Args : Arguments() {
            var host by long().compIdOnly()
            var type by enumBasic<ResetType>()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val event = events[e.host] ?: return iData.reply("Das Event läuft derzeit nicht!", ephemeral = true)
            event.reset(e.type)
        }
    }
}