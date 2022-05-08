package de.tectoast.emolga.utils.records;

import java.util.function.Function;

public record SorterData(String formulaRange, String pointRange, boolean directCompare,
                         Function<String, Integer> indexer, int... cols) {
}
