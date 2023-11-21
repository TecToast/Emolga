package de.tectoast.emolga

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.TestCommandData
import de.tectoast.emolga.utils.Constants

val defaultGuild by lazy { jda.getGuildById(Constants.G.MY)!! }
val defaultChannel by lazy { jda.getTextChannelById(Constants.TEST_TCID)!! }

inline fun testCommand(receiver: TestCommandData.() -> Unit) = with(TestCommandData(), receiver)
