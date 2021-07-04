package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.sql.ResultSetMetaData;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class PokedexManager extends DataManager {

    final StringColumn POKEMONNAME = new StringColumn("pokemonname", this);

    public PokedexManager() {
        super("pokedex");
    }

    public Pair<String, String> getDexEntry(String name) {
        return read(selectAll(POKEMONNAME.check(name.toLowerCase())), set -> {
            set.next();
            List<String> possible = new LinkedList<>();
            List<String> edis = new LinkedList<>();
            ResultSetMetaData meta = set.getMetaData();
            for (int i = 2; i <= 37; i++) {
                String s = set.getString(i);
                if (s != null) {
                    possible.add(s);
                    edis.add(meta.getColumnName(i));
                }
            }
            int index = new Random().nextInt(possible.size());
            return new ImmutablePair<>(possible.get(index), edis.get(index));
        });
    }
}
