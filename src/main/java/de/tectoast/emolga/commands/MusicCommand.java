package de.tectoast.emolga.commands;

public abstract class MusicCommand extends Command {

    public MusicCommand(String name, String help, long... guildIds) {
        super(name, help, CommandCategory.Music, guildIds);
        otherPrefix = true;
        addCustomChannel(712035338846994502L, 716221567079546983L, 735076688144105493L);
    }
}
