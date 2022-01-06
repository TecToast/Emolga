package de.tectoast.emolga.commands;

public abstract class PepeCommand extends Command {

    public PepeCommand(String name, String help) {
        super(name, help, CommandCategory.Pepe);
        this.allowedBotId = 849569577343385601L;
    }
}
