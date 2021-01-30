package de.tectoast.utils;


import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static de.tectoast.utils.Google.*;


public class RequestBuilder {

    private final ArrayList<Request> requests = new ArrayList<>();
    private final String sid;

    /**
     * Creates a RequestBuilder
     *
     * @param sid The ID of the sheet where the values should be written
     */
    public RequestBuilder(String sid) {
        this.sid = sid;
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
        requests.add(new Request().setRange(range).setSend(Collections.singletonList(body)).setValueInputOption(raw.length == 0 || !raw[0] ? "USER_ENTERED" : "RAW"));
        return this;
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
        requests.add(new Request().setRange(range).setSend(Collections.singletonList(Collections.singletonList(body))).setValueInputOption(raw.length == 0 || !raw[0] ? "USER_ENTERED" : "RAW"));
        return this;
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
        //System.out.println("raw = " + Arrays.toString(raw));
        requests.add(new Request().setRange(range).setSend(body).setValueInputOption(raw.length == 0 || !raw[0] ? "USER_ENTERED" : "RAW"));
        return this;
    }

    /**
     * Executes the request to the specified google sheet
     */
    public void execute() {
        refreshTokenIfNotPresent();
        executeInternally(false);
    }

    private List<ValueRange> getUserEntered() {
        return requests.stream().filter(r -> r.valueInputOption.equals("USER_ENTERED")).map(Request::build).collect(Collectors.toList());
    }

    private List<ValueRange> getRaw() {
        return requests.stream().filter(r -> r.valueInputOption.equals("RAW")).map(Request::build).collect(Collectors.toList());
    }

    private void executeInternally(boolean recursive) {
        try {
            List<ValueRange> userentered = getUserEntered();
            List<ValueRange> raw = getRaw();
            Sheets service = getSheetsService();
            if (!userentered.isEmpty())
                service.spreadsheets().values().batchUpdate(sid, new BatchUpdateValuesRequest().setData(userentered).setValueInputOption("USER_ENTERED")).execute();
            if (!raw.isEmpty())
                service.spreadsheets().values().batchUpdate(sid, new BatchUpdateValuesRequest().setData(raw).setValueInputOption("RAW")).execute();
        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            if (recursive) {
                new IllegalArgumentException().printStackTrace();
                return;
            }
            generateAccessToken();
            executeInternally(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Request {
        private String range;
        private List<List<Object>> send;
        private String valueInputOption;

        public Request setRange(String range) {
            this.range = range;
            return this;
        }

        public Request setSend(List<List<Object>> send) {
            this.send = send;
            return this;
        }

        public Request setValueInputOption(String valueInputOption) {
            this.valueInputOption = valueInputOption;
            return this;
        }

        public ValueRange build() {
            return new ValueRange().setValues(send).setRange(range);
        }

    }
}
