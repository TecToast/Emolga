package de.tectoast.emolga.utils;

import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings("UnusedReturnValue")
public class RequestGetter {
    private static final Logger logger = LoggerFactory.getLogger(RequestGetter.class);
    private final String sid;
    private final List<String> ranges = new LinkedList<>();

    public RequestGetter(String sid) {
        this.sid = sid;
    }

    public RequestGetter addRange(String range) {
        ranges.add(range);
        return this;
    }

    public List<List<List<String>>> execute() {
        try {
            Spreadsheet sh = Google.getSheetsService().spreadsheets().get(sid).setIncludeGridData(true).setRanges(ranges).execute();
            HashMap<String, AtomicInteger> map = new HashMap<>();
            List<List<List<String>>> ret = new ArrayList<>(ranges.size());
            for (String range : ranges) {
                logger.info("range = {}", range);
                String sheetname = range.split("!")[0];
                ret.add(sh.getSheets().stream().filter(s -> s.getProperties().getTitle().equals(sheetname)).findFirst().orElse(null).getData()
                        .get(map.computeIfAbsent(sheetname, i -> new AtomicInteger(-1)).incrementAndGet())
                        .getRowData().stream().map(rd -> rd.getValues().stream().filter(cd -> cd.getEffectiveValue() != null).map(cd -> {
                            ExtendedValue v = cd.getEffectiveValue();
                            if (v.getStringValue() != null) return v.getStringValue();
                            double d = v.getNumberValue();
                            if (d % 1.0 != 0)
                                return String.format("%s", d);
                            else
                                return String.format("%.0f", d);
                        }).collect(Collectors.toList())).collect(Collectors.toList()));
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
