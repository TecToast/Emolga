package de.tectoast.emolga

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.commands.draft.during.*
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.litote.kmongo.coroutine.insertOne
import kotlin.coroutines.suspendCoroutine

val defaultGuild by lazy { jda.getGuildById(Constants.G.MY)!! }
val defaultChannel by lazy { jda.getTextChannelById(Constants.TEST_TCID)!! }

inline fun testCommand(receiver: TestCommandData.() -> Unit) = with(TestCommandData(), receiver)

suspend fun <T> CompletableDeferred<T>.awaitTimeout(timeout: Long = 3000): T {
    return withTimeout(timeout) { await() }
}

suspend fun createDraft(
    name: String,
    playerCount: Int,
    rounds: Int,
    generateDraftOrder: Boolean = true,
    guild: Long = Constants.G.ASL
) {
    db.drafts.insertOne(("{type: 'Default', leaguename:'TEST$name', table: " +
            "${myJSON.encodeToString(List(playerCount) { 10_000_000_000 + it })}, guild: $guild".condAppend(
                generateDraftOrder,
                ",originalorder: ${
                    myJSON.encodeToString(
                        buildMap<Int, List<Int>> {
                            val indices = (0..<playerCount).map { it }
                            for (i in 1..rounds) {
                                put(i, if (i % 2 == 0) get(i - 1)!!.asReversed() else indices.shuffled())
                            }
                        }
                    )
                }") + "}").also { println(it) })

}

suspend fun startDraft(name: String) {
    db.league("TEST$name").startDraft(defaultChannel, fromFile = false, switchDraft = false)
}

fun enableReplyRedirect(channel: MessageChannel = defaultChannel) {
    redirectTestCommandLogsToChannel = channel
}

suspend fun pick(name: String) {
    testCommand {
        val guildId = tc.let { League.onlyChannel(it)?.guild } ?: gid
        val draftName = NameConventionsDB.getDiscordTranslation(
            name, guildId, english = Tierlist[guildId].isEnglish
        )!!
        PickCommand.exec(PickCommandArgs(draftName))
    }
}

suspend fun randomPick(tier: String) {
    testCommand {
        RandomPickCommand.exec(RandomPickCommandArgs(tier))
    }
}

suspend fun movePick() {
    testCommand {
        MoveCommand.exec(NoCommandArgs)
    }
}
suspend fun keepAlive() = suspendCoroutine<Unit> { }
