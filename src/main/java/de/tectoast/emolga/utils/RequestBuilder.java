package de.tectoast.emolga.utils;


import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.*;
import static de.tectoast.emolga.utils.Google.getSheetsService;


@SuppressWarnings("UnusedReturnValue")
public class RequestBuilder {

    private static final Logger logger = LoggerFactory.getLogger(RequestBuilder.class);

    private final ArrayList<MyRequest> requests = new ArrayList<>();
    private final String sid;
    private boolean executed = false;
    private Runnable runnable;
    private long delay = 0;
    private boolean suppressMessages = false;
    private String[] additionalSheets;

    /**
     * Creates a RequestBuilder
     *
     * @param sid The ID of the sheet where the values should be written
     */
    public RequestBuilder(String sid) {
        this.sid = sid;
    }

    public static void updateSingle(String sid, String range, Object value, boolean... raw) {
        updateRow(sid, range, Collections.singletonList(value), raw);
    }

    public static void updateRow(String sid, String range, List<Object> values, boolean... raw) {
        updateAll(sid, range, Collections.singletonList(values), raw);
    }

    public static void updateAll(String sid, String range, List<List<Object>> values, boolean... raw) {
        new Thread(() -> {
            try {
                getSheetsService().spreadsheets().values().update(sid, range, new ValueRange().setValues(values)).setValueInputOption(raw.length == 0 || !raw[0] ? "USER_ENTERED" : "RAW").execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "ReqBuilder").start();
    }

    public static void batchUpdate(String sid, Request... requests) {
        if (requests.length == 0) return;
        try {
            getSheetsService().spreadsheets().batchUpdate(sid, new BatchUpdateSpreadsheetRequest()
                    .setRequests(Arrays.asList(requests))).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getColumnFromRange(String range) {
        char[] chars = range.replaceAll("[^a-zA-Z]", "").toCharArray();
        if (chars.length == 1) return chars[0] - 65;
        return (chars[0] - 64) * 26 + (chars[1] - 65);
    }

    public static int getRowFromRange(String range) {
        return Integer.parseInt(range.replaceAll("[^\\d]", "")) - 1;
    }

    public static GridRange buildGridRange(String expr, int sheetId) {
        String[] split = expr.split(":");
        String s1 = split[0];
        String s2 = split.length == 1 ? s1 : split[1];
        GridRange r = new GridRange();
        r.setSheetId(sheetId);
        r.setStartColumnIndex(getColumnFromRange(s1))
                .setStartRowIndex(getRowFromRange(s1));
        r.setEndColumnIndex(getColumnFromRange(s2) + 1)
                .setEndRowIndex(getRowFromRange(s2) + 1);
        try {
            logger.info("r = {}", r.toPrettyString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return r;
    }

    public RequestBuilder withRunnable(Runnable r) {
        this.runnable = r;
        return this;
    }

    public RequestBuilder withRunnable(Runnable r, long delay) {
        this.runnable = r;
        this.delay = delay;
        return this;
    }

    public RequestBuilder suppressMessages() {
        this.suppressMessages = true;
        return this;
    }

    public RequestBuilder withAdditionalSheets(String... additionalSheets) {
        this.additionalSheets = additionalSheets;
        return this;
    }

    /**
     * Adds a single object to the builder
     *
     * @param range The range, where the object should be written
     * @param body  The single object that should be written
     * @param raw   optional argument, which makes the request raw (if true) or user entered (if false or null)
     * @return this RequestBuilder
     */
    public RequestBuilder addSingle(String range, Object body, boolean... raw) {
        return addRow(range, Collections.singletonList(body), raw);
    }

    /**
     * Adds a row of objects to the builder
     *
     * @param range The range, where the row should be written
     * @param body  The row that should be written
     * @param raw   optional argument, which makes the request raw (if true) or user entered (if false or null)
     * @return this RequestBuilder
     */
    @SuppressWarnings("UnusedReturnValue")
    public RequestBuilder addRow(String range, List<Object> body, boolean... raw) {
        return addAll(range, Collections.singletonList(body), raw);
    }

    /**
     * Adds a matrix of objects to the builder
     *
     * @param range The range, where the objects should be written
     * @param body  The matrix that should be written
     * @param raw   optional argument, which makes the request raw (if true) or user entered (if false or null)
     * @return this RequestBuilder
     */
    public RequestBuilder addAll(String range, List<List<Object>> body, boolean... raw) {
        requests.add(new MyRequest().setRange(range).setSend(body).setValueInputOption(raw.length == 0 || !raw[0] ? ValueInputOption.USER_ENTERED : ValueInputOption.RAW));
        return this;
    }

    public RequestBuilder addColumn(String range, List<Object> body, boolean... raw) {
        return addAll(range, body.stream().map(Collections::singletonList).collect(Collectors.toList()));
    }

    /**
     * Adds one or multiple batch request(s) to the builder
     *
     * @param requests The request(s) that should be sent
     * @return this RequestBuilder
     */
    public RequestBuilder addBatch(Request... requests) {
        Arrays.stream(requests).map(r -> new MyRequest().setRequest(r)).forEach(this.requests::add);
        return this;
    }

    public RequestBuilder addBGColorChange(int sheetId, String range, Color c) {
        String[] split = range.split(":");
        String s1 = split[0];
        String s2 = split.length == 1 ? s1 : split[1];
        return addBatch(new Request().setUpdateCells(new UpdateCellsRequest()
                .setRange(buildGridRange(range, sheetId))
                .setFields("userEnteredFormat.backgroundColor")
                .setRows(getCellsAsRowData(
                        new CellData().setUserEnteredFormat(new CellFormat().setBackgroundColor(c)), getColumnFromRange(s2) - getColumnFromRange(s1) + 1, getRowFromRange(s2) - getRowFromRange(s1) + 1
                ))));
    }

    public RequestBuilder addBGColorChange(int sheetId, int x, int y, Color c) {
        return addBGColorChange(sheetId, getAsXCoord(x) + y, c);
    }

    public RequestBuilder addNoteChange(int sheetId, String range, String note) {
        String[] split = range.split(":");
        String s1 = split[0];
        String s2 = split.length == 1 ? s1 : split[1];
        return addBatch(new Request().setUpdateCells(new UpdateCellsRequest()
                .setRange(buildGridRange(range, sheetId))
                .setFields("note")
                .setRows(getCellsAsRowData(
                        new CellData().setNote(note), getColumnFromRange(s2) - getColumnFromRange(s1) + 1, getRowFromRange(s2) - getRowFromRange(s1) + 1
                ))));
    }

    public RequestBuilder addHorizontalAlignmentChange(int sheetId, String range, String alignment) {
        String[] split = range.split(":");
        String s1 = split[0];
        String s2 = split.length == 1 ? s1 : split[1];
        return addBatch(new Request().setUpdateCells(new UpdateCellsRequest()
                .setRange(buildGridRange(range, sheetId))
                .setFields("userEnteredFormat.horizontalAlignment")
                .setRows(getCellsAsRowData(
                        new CellData().setUserEnteredFormat(new CellFormat().setHorizontalAlignment(alignment)), getColumnFromRange(s2) - getColumnFromRange(s1) + 1, getRowFromRange(s2) - getRowFromRange(s1) + 1
                ))));
    }

    public RequestBuilder addVerticalAlignmentChange(int sheetId, String range, String alignment) {
        String[] split = range.split(":");
        String s1 = split[0];
        String s2 = split.length == 1 ? s1 : split[1];
        return addBatch(new Request().setUpdateCells(new UpdateCellsRequest()
                .setRange(buildGridRange(range, sheetId))
                .setFields("userEnteredFormat.verticalAlignment")
                .setRows(getCellsAsRowData(
                        new CellData().setUserEnteredFormat(new CellFormat().setVerticalAlignment(alignment)), getColumnFromRange(s2) - getColumnFromRange(s1) + 1, getRowFromRange(s2) - getRowFromRange(s1) + 1
                ))));
    }

    public RequestBuilder addFontChange(int sheetId, String range, String font) {
        String[] split = range.split(":");
        String s1 = split[0];
        String s2 = split.length == 1 ? s1 : split[1];
        return addBatch(new Request().setUpdateCells(new UpdateCellsRequest()
                .setRange(buildGridRange(range, sheetId))
                .setFields("userEnteredFormat.textFormat.fontFamily")
                .setRows(getCellsAsRowData(
                        new CellData().setUserEnteredFormat(new CellFormat().setTextFormat(new TextFormat().setFontFamily(font))), getColumnFromRange(s2) - getColumnFromRange(s1) + 1, getRowFromRange(s2) - getRowFromRange(s1) + 1
                ))));
    }

    public RequestBuilder addStrikethroughChange(int sheetId, String range, boolean strikethrough) {
        String[] split = range.split(":");
        String s1 = split[0];
        String s2 = split.length == 1 ? s1 : split[1];
        return addBatch(new Request().setUpdateCells(new UpdateCellsRequest()
                .setRange(buildGridRange(range, sheetId))
                .setFields("userEnteredFormat.textFormat.strikethrough")
                .setRows(getCellsAsRowData(
                        new CellData().setUserEnteredFormat(new CellFormat().setTextFormat(new TextFormat().setStrikethrough(strikethrough))), getColumnFromRange(s2) - getColumnFromRange(s1) + 1, getRowFromRange(s2) - getRowFromRange(s1) + 1
                ))));
    }

    public RequestBuilder addStrikethroughChange(int sheetId, int x, int y, boolean strikethrough) {
        return addStrikethroughChange(sheetId, getAsXCoord(x) + y, strikethrough);
    }

    public RequestBuilder addFGColorChange(int sheetId, String range, Color c) {
        String[] split = range.split(":");
        String s1 = split[0];
        String s2 = split.length == 1 ? s1 : split[1];
        return addBatch(new Request().setUpdateCells(new UpdateCellsRequest()
                .setRange(buildGridRange(range, sheetId))
                .setFields("userEnteredFormat.textFormat.foregroundColor")
                .setRows(getCellsAsRowData(
                        new CellData().setUserEnteredFormat(new CellFormat().setTextFormat(new TextFormat().setForegroundColor(c))), getColumnFromRange(s2) - getColumnFromRange(s1) + 1, getRowFromRange(s2) - getRowFromRange(s1) + 1
                ))));
    }

    public RequestBuilder addFGColorChange(int sheetId, int x, int y, Color c) {
        return addFGColorChange(sheetId, getAsXCoord(x) + y, c);
    }


    private List<ValueRange> getUserEntered() {
        return requests.stream().filter(r -> r.valueInputOption.equals(ValueInputOption.USER_ENTERED)).map(MyRequest::build).collect(Collectors.toList());
    }

    private List<ValueRange> getRaw() {
        return requests.stream().filter(r -> r.valueInputOption.equals(ValueInputOption.RAW)).map(MyRequest::build).collect(Collectors.toList());
    }

    private List<Request> getBatch() {
        return requests.stream().filter(r -> r.valueInputOption.equals(ValueInputOption.BATCH)).map(MyRequest::buildBatch).collect(Collectors.toList());
    }

    /**
     * Executes the request to the specified google sheet
     */
    public void execute() {
        if (executed)
            throw new IllegalStateException("Already executed RequestBuilder with requests:\nsid = " + this.sid + "\n" + requests);

        List<ValueRange> userentered = getUserEntered();
        List<ValueRange> raw = getRaw();
        List<Request> batch = getBatch();
        Sheets service = getSheetsService();
        List<Thread> list = new LinkedList<>();

        list.add(new Thread(() -> {
            if (!userentered.isEmpty()) {
                if (!suppressMessages)
                    for (int i = 0; i < userentered.size(); i++) {
                        ValueRange range = userentered.get(i);
                        logger.info("{}: {} -> {}", i, range.getRange(), range.getValues());
                    }
                try {
                    service.spreadsheets().values().batchUpdate(sid, new BatchUpdateValuesRequest().setData(userentered).setValueInputOption("USER_ENTERED")).execute();
                    if (additionalSheets != null) {
                        for (String sidd : additionalSheets) {
                            service.spreadsheets().values().batchUpdate(sidd, new BatchUpdateValuesRequest().setData(userentered).setValueInputOption("USER_ENTERED")).execute();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    sendStacktraceToMe(e);
                }
            }
        }, "ReqBuilder User"));
        list.add(new Thread(() -> {
            if (!raw.isEmpty()) {
                try {
                    service.spreadsheets().values().batchUpdate(sid, new BatchUpdateValuesRequest().setData(raw).setValueInputOption("RAW")).execute();
                    if (additionalSheets != null) {
                        for (String sidd : additionalSheets) {
                            service.spreadsheets().values().batchUpdate(sidd, new BatchUpdateValuesRequest().setData(raw).setValueInputOption("RAW")).execute();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    sendStacktraceToMe(e);
                }
            }
        }, "ReqBuilder Raw"));
        list.add(new Thread(() -> {
            if (!batch.isEmpty()) {
                try {
                    service.spreadsheets().batchUpdate(sid, new BatchUpdateSpreadsheetRequest().setRequests(batch)).execute();
                    if (additionalSheets != null) {
                        for (String sidd : additionalSheets) {
                            service.spreadsheets().batchUpdate(sidd, new BatchUpdateSpreadsheetRequest().setRequests(batch)).execute();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    sendStacktraceToMe(e);
                }
            }
        }, "ReqBuilder Batch"));
        list.forEach(Thread::start);
        //noinspection IfStatementWithIdenticalBranches
        if (runnable != null) {
            try {
                for (Thread thread : list) {
                    thread.join();
                }
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runnable.run();
            executed = true;
        } else
            executed = true;
    }

    private enum ValueInputOption {
        RAW,
        USER_ENTERED,
        BATCH
    }

    private static class MyRequest {
        private String range;
        private List<List<Object>> send;
        private ValueInputOption valueInputOption;
        private Request request;

        public MyRequest setRange(String range) {
            this.range = range;
            return this;
        }

        public MyRequest setSend(List<List<Object>> send) {
            this.send = send;
            return this;
        }

        public MyRequest setRequest(Request request) {
            this.request = request;
            this.valueInputOption = ValueInputOption.BATCH;
            return this;
        }

        public MyRequest setValueInputOption(ValueInputOption valueInputOption) {
            this.valueInputOption = valueInputOption;
            return this;
        }

        public ValueRange build() {
            return new ValueRange().setValues(send).setRange(range);
        }

        public Request buildBatch() {
            return request;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "range='" + range + '\'' +
                    ", send=" + send +
                    ", valueInputOption='" + valueInputOption + '\'' +
                    '}';
        }
    }
}
