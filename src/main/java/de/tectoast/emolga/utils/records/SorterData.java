package de.tectoast.emolga.utils.records;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public record SorterData(List<String> formulaRange, List<String> pointRange, boolean directCompare,
                         Function<String, Integer> indexer, int... cols) {
    public SorterData(String formulaRange, String pointRange, boolean directCompare, Function<String, Integer> indexer, int... cols) {
        this(Collections.singletonList(formulaRange), Collections.singletonList(pointRange),
                directCompare, indexer, cols);
    }
}
