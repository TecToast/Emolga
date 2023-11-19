package de.tectoast.emolga

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.utils.Constants

val defaultGuild by lazy { jda.getGuildById(Constants.G.MY)!! }
val defaultChannel by lazy { jda.getTextChannelById(1171544197573116004)!! }
