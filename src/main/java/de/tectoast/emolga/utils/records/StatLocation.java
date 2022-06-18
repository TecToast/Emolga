package de.tectoast.emolga.utils.records;

import static de.tectoast.emolga.commands.Command.getAsXCoord;

@SuppressWarnings("unused")
public record StatLocation(String sheet, String x, int y) {
    private static final StatLocation INVALD = new StatLocation(null, 0, 0);

    public StatLocation(String sheet, int x, int y) {
        this(sheet, getAsXCoord(x), y);
    }

    public static StatLocation invalid() {
        return INVALD;
    }

    public boolean isValid() {
        return sheet != null;
    }
}