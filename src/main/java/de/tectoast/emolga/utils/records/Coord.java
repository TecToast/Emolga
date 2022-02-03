package de.tectoast.emolga.utils.records;

public record Coord(int x, int y) {
    public boolean valid() {
        return x != -1;
    }
}
