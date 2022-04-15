package de.tectoast.emolga.commands;

import de.tectoast.emolga.utils.Constants;

public abstract class RPICommand extends Command {
    public RPICommand(String name, String help) {
        super(name, help, CommandCategory.Flo, Constants.MYSERVER);
    }
}
