package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.BooleanColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;

import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.Objects;

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
        setColumns(ENGLISHID, GERMANID, ENGLISHNAME, GERMANNAME, TYPE, MODIFICATION, ISNICK, FORME);
    }

    public ResultSet getTranslation(String id, String mod, boolean checkOnlyEnglish) {
        //String s = "(englishid=\"" + id + "\" or germanid=\"" + id + "\")" + (mod != null ? " and (modification=\"" + mod + "\"" + (!mod.equals("default") ? " or modification=\"default\"" : "") + ")" : "");

        return Database.select(selectAll(
                and(
                        or(ENGLISHID.check(id), GERMANID.check(id), !checkOnlyEnglish),
                        or(MODIFICATION.check("default"), MODIFICATION.check(mod), !Objects.equals(mod, "default"))
                        , Objects.nonNull(mod))
        ));
    }

    public void addNick(String nick, Command.Translation t) {
        String s = Command.toSDName(nick);
        insertOrUpdateOld(ENGLISHID, s, s, s, t.getOtherLang(), t.getTranslation(), t.getType().getId(), "default", true, null);
    }

    public boolean removeNick(String nick) {
        String sd = Command.toSDName(nick);
        Command.removeNickFromCache(sd);
        return delete(ENGLISHID.check(sd)) != 0;
    }

    public void removeDuplicates() {
        readWrite(selectAll(TYPE.check("trainer")), set -> {
            LinkedList<String> l = new LinkedList<>();
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

    public void cap(String name, String id) {
        update(ENGLISHID, id, CAP, true);
    }
}
