package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.IntColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;

public class DasorUsageManager extends DataManager {

    final StringColumn POKEMON = new StringColumn("pokemon", this);
    final IntColumn USES = new IntColumn("uses", this);

    public DasorUsageManager() {
        super("dasorusage");
        setColumns(POKEMON, USES);
    }

    public void addPokemon(String pokemon) {
        insertOrUpdate(POKEMON, pokemon, results -> results.updateInt("uses", results.getInt("uses") + 1), pokemon, 1);
    }
    public MessageEmbed buildMessage(){
        return new EmbedBuilder()
                .setTitle("Dasor Statistik mit coolen Mons")
                .setDescription(read(selectAll() + " ORDER BY uses DESC", results -> {
                    StringBuilder b = new StringBuilder();
                    while (results.next()){
                        b.append(results.getString("pokemon")).append(": ")
                                .append(results.getInt("uses")).append("\n");
                    }
                    return b.toString();
                }))
                .setColor(Color.CYAN)
                .build();
    }
}
