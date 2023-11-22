package de.tectoast.emolga

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.TestCommandData
import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.commands.myJSON
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
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
                            val indices = (0..playerCount).map { it }
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

suspend fun keepAlive() = suspendCoroutine<Unit> { }
