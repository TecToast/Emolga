package de.tectoast.utils;


import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import de.tectoast.commands.Command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static de.tectoast.utils.Google.getSheetsService;


public class RequestBuilder {

    private final ArrayList<Request> requests = new ArrayList<>();
    private final String sid;
    private boolean executed = false;

    /**
     * Creates a RequestBuilder
     *
     * @param sid The ID of the sheet where the values should be written
     */
    public RequestBuilder(String sid) {
        this.sid = sid;
    }


    public static void updateSingle(String sid, String range, Object value, boolean raw) {
        updateRow(sid, range, Collections.singletonList(value), raw);
    }

    public static void updateRow(String sid, String range, List<Object> values, boolean raw) {
        updateAll(sid, range, Collections.singletonList(values), raw);
    }

    public static void updateAll(String sid, String range, List<List<Object>> values, boolean raw) {
        new Thread(() -> {
            try {
                getSheetsService().spreadsheets().values().update(sid, range, new ValueRange().setValues(values)).setValueInputOption(raw ? "RAW" : "USER_ENTERED").execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    public static void batchUpdate(String sid, com.google.api.services.sheets.v4.model.Request... requests) {
        if (requests.length == 0) return;
        try {
            getSheetsService().spreadsheets().batchUpdate(sid, new BatchUpdateSpreadsheetRequest()
                    .setRequests(Arrays.asList(requests))).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a single object to the builder
     *
     * @param range The range, where the object should be written
     * @param body  The single object that should be written
     * @param raw   optional argument, which makes the request raw (if true) or user entered (if false)
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
     * @param raw   optional argument, which makes the request raw (if true) or user entered (if false)
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
     * @param raw   optional argument, which makes the request raw (if true) or user entered (if false)
     * @return this RequestBuilder
     */
    public RequestBuilder addAll(String range, List<List<Object>> body, boolean... raw) {
        requests.add(new Request().setRange(range).setSend(body).setValueInputOption(raw.length == 0 || !raw[0] ? ValueInputOption.USER_ENTERED : ValueInputOption.RAW));
        return this;
    }

    /**
     * Adds a or multiple batch request(s) to the builder
     *
     * @param requests The request(s) that should be sent
     * @return this RequestBuilder
     */
    public RequestBuilder addBatch(com.google.api.services.sheets.v4.model.Request... requests) {
        Arrays.stream(requests).map(r -> new Request().setRequest(r)).forEach(this.requests::add);
        return this;
    }

    private List<ValueRange> getUserEntered() {
        return requests.stream().filter(r -> r.valueInputOption.equals(ValueInputOption.USER_ENTERED)).map(Request::build).collect(Collectors.toList());
    }

    private List<ValueRange> getRaw() {
        return requests.stream().filter(r -> r.valueInputOption.equals(ValueInputOption.RAW)).map(Request::build).collect(Collectors.toList());
    }

    private List<com.google.api.services.sheets.v4.model.Request> getBatch() {
        return requests.stream().filter(r -> r.valueInputOption.equals(ValueInputOption.BATCH)).map(Request::buildBatch).collect(Collectors.toList());
    }


    /**
     * Executes the request to the specified google sheet
     */
    public void execute() {
        if (executed) throw new IllegalStateException("Already executed RequestBuilder with requests:\nsid = " + this.sid + "\n" + requests.toString());
        List<ValueRange> userentered = getUserEntered();
        List<ValueRange> raw = getRaw();
        List<com.google.api.services.sheets.v4.model.Request> batch = getBatch();
        Sheets service = getSheetsService();
        new Thread(() -> {
            if (!userentered.isEmpty()) {
                try {
                    service.spreadsheets().values().batchUpdate(sid, new BatchUpdateValuesRequest().setData(userentered).setValueInputOption("USER_ENTERED")).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    Command.sendStacktraceToMe(e);
                }
            }
        }).start();
        new Thread(() -> {
            if (!raw.isEmpty()) {
                try {
                    service.spreadsheets().values().batchUpdate(sid, new BatchUpdateValuesRequest().setData(raw).setValueInputOption("RAW")).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    Command.sendStacktraceToMe(e);
                }
            }
        }).start();
        new Thread(() -> {
            if (!batch.isEmpty()) {
                try {
                    service.spreadsheets().batchUpdate(sid, new BatchUpdateSpreadsheetRequest().setRequests(batch)).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    Command.sendStacktraceToMe(e);
                }
            }
        }).start();
        executed = true;
    }

    private enum ValueInputOption {
        RAW,
        USER_ENTERED,
        BATCH
    }

    private static class Request {
        private String range;
        private List<List<Object>> send;
        private ValueInputOption valueInputOption;
        private com.google.api.services.sheets.v4.model.Request request;

        public Request setRange(String range) {
            this.range = range;
            return this;
        }

        public Request setSend(List<List<Object>> send) {
            this.send = send;
            return this;
        }

        public Request setRequest(com.google.api.services.sheets.v4.model.Request request) {
            this.request = request;
            this.valueInputOption = ValueInputOption.BATCH;
            return this;
        }

        public Request setValueInputOption(ValueInputOption valueInputOption) {
            this.valueInputOption = valueInputOption;
            return this;
        }

        public ValueRange build() {
            return new ValueRange().setValues(send).setRange(range);
        }

        public com.google.api.services.sheets.v4.model.Request buildBatch() {
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
