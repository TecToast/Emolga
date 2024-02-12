package de.tectoast.emolga

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.NoCommandArgs
import de.tectoast.emolga.commands.TestCommandData
import de.tectoast.emolga.commands.draft.during.*
import de.tectoast.emolga.commands.myJSON
import de.tectoast.emolga.commands.redirectTestCommandLogsToChannel
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
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

suspend fun createTestDraft(
    name: String,
    playerCount: Int,
    rounds: Int,
    originalorder: Map<Int, List<Int>> = buildMap {
        val indices = (0..<playerCount).map { it }
        for (i in 1..rounds) {
            put(i, if (i % 2 == 0) get(i - 1)!!.asReversed() else indices.shuffled())
        }
    },
    guild: Long = Constants.G.ASL,
    hardcodedUserIds: Map<Int, Long> = emptyMap()
) {
    db.drafts.insertOne(
        myJSON.encodeToString(
            DemoLeague(
                type = "Default",
                leaguename = "TEST$name",
                table = List(playerCount) { hardcodedUserIds[it] ?: (10_000_000_000 + it) },
                guild = guild,
                originalorder = originalorder
            )
        )
    )

}

@Suppress("unused")
@Serializable
private class DemoLeague(
    val type: String,
    val leaguename: String,
    val table: List<Long>,
    val guild: Long,
    val originalorder: Map<Int, List<Int>>
)

suspend fun startTestDraft(name: String) {
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
