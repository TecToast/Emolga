package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class CalcCommand extends Command {

    final ScriptEngineManager mgr = new ScriptEngineManager();
    final ScriptEngine engine = mgr.getEngineByName("JavaScript");
    public CalcCommand() {
        super("calc", "Berechnet die angegebene Formel", CommandCategory.Various, Constants.BSID, Constants.GAMERSQUAD);
        setArgumentTemplate(ArgumentManagerTemplate.noSpecifiedArgs("!calc <Matheformel>", "!calc 1+1"));
        disable();
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay().substring(6);
        try {
            tco.sendMessage("Die Lösung von `" + msg + "` ist: **" + engine.eval(msg) + "**").queue();
        } catch (ScriptException scriptException) {
            tco.sendMessage("Das ist keine valide Formel!").queue();
            //scriptException.printStackTrace();
        }
    }
}
