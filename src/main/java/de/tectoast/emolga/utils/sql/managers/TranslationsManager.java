package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.BooleanColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static de.tectoast.emolga.utils.sql.base.Condition.and;
import static de.tectoast.emolga.utils.sql.base.Condition.or;

public class TranslationsManager extends DataManager {

    final StringColumn ENGLISHID = new StringColumn("englishid", this);
    final StringColumn GERMANID = new StringColumn("germanid", this);
    final StringColumn ENGLISHNAME = new StringColumn("englishname", this);
    final StringColumn GERMANNAME = new StringColumn("germanname", this);
    final StringColumn TYPE = new StringColumn("type", this);
    final StringColumn MODIFICATION = new StringColumn("modification", this);
    final BooleanColumn ISNICK = new BooleanColumn("isnick", this);
    final StringColumn FORME = new StringColumn("forme", this);
    final BooleanColumn CAP = new BooleanColumn("cap", this);


    public TranslationsManager() {
        super("translations");
        setColumns(ENGLISHID, GERMANID, ENGLISHNAME, GERMANNAME, TYPE, MODIFICATION, ISNICK, FORME, CAP);
    }

    public ResultSet getTranslation(String id, boolean checkOnlyEnglish) {
        return read(selectAll(
                and(
                        or(ENGLISHID.check(id), GERMANID.check(id), !checkOnlyEnglish),
                        CAP.check(0)
                )
        ), r -> r);
    }

    public ResultSet getTranslationList(Collection<String> l) {
        return read(selectAll("(%s)".formatted(l.stream().map(str -> "englishid=\"" + Command.toSDName(str) + "\"").collect(Collectors.joining(" or ")))), r -> r);
    }

    public void addNick(String nick, Command.Translation t) {
        String s = Command.toSDName(nick);
        replaceIfExists(s, s, t.getOtherLang(), t.getTranslation(), t.getType().getId(), "default", true, null, false);
    }

    public boolean removeNick(String nick) {
        String sd = Command.toSDName(nick);
        Command.removeNickFromCache(sd);
        return delete(and(ENGLISHID.check(sd), ISNICK.check(true))) != 0;
    }

    public void removeDuplicates() {
        readWrite(selectAll(TYPE.check("trainer")), set -> {
            List<String> l = new LinkedList<>();
            while (set.next()) {
                String value = ENGLISHID.getValue(set);
                if (l.contains(value)) {
                    set.deleteRow();
                } else {
                    l.add(value);
                }
            }
        });
    }
}
