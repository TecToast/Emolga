package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.records.DexEntry;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSetMetaData;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class PokedexManager extends DataManager {

    private static final Logger logger = LoggerFactory.getLogger(PokedexManager.class);
    private static final Pattern NO_CHARS = Pattern.compile("[^A-Za-z]");

    final StringColumn POKEMONNAME = new StringColumn("pokemonname", this);

    public PokedexManager() {
        super("pokedex");
    }

    public DexEntry getDexEntry(String name) {
        logger.info(name);
        return read(selectAll(POKEMONNAME.check(NO_CHARS.matcher(name).replaceAll("").toLowerCase())), set -> {
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
            return new DexEntry(possible.get(index), edis.get(index));
        });
    }
}
