package de.tectoast.emolga.buttons.buttonsaves;

import java.util.List;

public class PrismaTeam {
    private final List<String> mons;
    private final int index;
    private int x = 0;

    public PrismaTeam(List<String> mons, int index) {
        this.mons = mons;
        this.index = index;
    }

    public PokemonData nextMon() {
        int i = x++;
        return new PokemonData(mons.get(i), 13 - i);
    }

    public int getIndex() {
        return index;
    }

    public record PokemonData(String pokemon, int ycoord) {
    }
}
