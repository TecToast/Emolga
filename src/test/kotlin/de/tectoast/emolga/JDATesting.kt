package de.tectoast.emolga

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.TestCommandData
import de.tectoast.emolga.utils.Constants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

val defaultGuild by lazy { jda.getGuildById(Constants.G.MY)!! }
val defaultChannel by lazy { jda.getTextChannelById(Constants.TEST_TCID)!! }

inline fun testCommand(receiver: TestCommandData.() -> Unit) = with(TestCommandData(), receiver)

suspend fun <T> CompletableDeferred<T>.awaitTimeout(timeout: Long = 3000): T {
    return withTimeout(timeout) { await() }
}
