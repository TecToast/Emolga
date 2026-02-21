package de.tectoast.emolga

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.TestInteractionData
import de.tectoast.emolga.features.league.draft.MoveCommand
import de.tectoast.emolga.features.league.draft.PickCommand
import de.tectoast.emolga.features.league.draft.RandomPick
import de.tectoast.emolga.features.redirectTestCommandLogsToChannel
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.myJSON
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.litote.kmongo.coroutine.insertOne

val defaultGuild by lazy { jda.getGuildById(Constants.G.MY)!! }
val defaultChannel by lazy { jda.getTextChannelById(Constants.TEST_TCID)!! }
val defaultCategory by lazy { jda.getCategoryById(Constants.TEST_CATID)!! }

suspend fun createChannel(name: String): TextChannel {
    return defaultGuild.getTextChannelsByName(name, true).firstOrNull() ?: defaultCategory.createTextChannel(name)
        .await()
}

inline fun testCommand(receiver: TestInteractionData.() -> Unit) = with(TestInteractionData(), receiver)

@Suppress("UNUSED_PARAMETER") // used to disable the command
inline fun xtestCommand(receiver: TestInteractionData.() -> Unit) = Unit

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
): suspend () -> League {
    mdb.league.insertOne(
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
    return { mdb.league("TEST$name") }
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
    League.executeOnFreshLock("TEST$name") {
        startDraft(defaultChannel, fromFile = false, switchDraft = false)
    }
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
        PickCommand.exec {
            this.pokemon = draftName
        }
    }
}

suspend fun randomPick(tier: String) {
    testCommand {
        RandomPick.Command.exec {
            this.tier = tier
        }
    }
}

suspend fun movePick() {
    testCommand {
        MoveCommand.exec()
    }
}

suspend fun keepAlive() = suspendCancellableCoroutine<Unit> { }
