package de.tectoast.emolga.commands;

import net.dv8tion.jda.api.entities.Message;

public class PrivateCommandEvent extends GenericCommandEvent {
    public PrivateCommandEvent(Message m) {
        super(m);
    }
}
