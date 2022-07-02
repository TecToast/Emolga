package de.tectoast.emolga.commands

abstract class MusicCommand(name: String, help: String, vararg guildIds: Long) :
    Command(name, help, CommandCategory.Music, *guildIds) {
    init {
        otherPrefix = true
        addCustomChannel(712035338846994502L, 716221567079546983L, 735076688144105493L)
    }
}