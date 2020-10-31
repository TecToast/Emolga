package de.Flori.utils;


import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static de.Flori.utils.Google.generateAccessToken;
import static de.Flori.utils.Google.getSheetsService;


public class RequestBuilder {

    private final ArrayList<Request> requests = new ArrayList<>();
    private final String sid;

    public RequestBuilder(String sid) {
        this.sid = sid;
    }


    public RequestBuilder addRow(String range, List<Object> body, boolean... raw) {
        requests.add(new Request().setRange(range).setSend(Collections.singletonList(body)).setValueInputOption(raw.length == 0 || !raw[0] ? "USER_ENTERED" : "RAW"));
        return this;
    }

    public RequestBuilder addSingle(String range, Object body, boolean... raw) {
        requests.add(new Request().setRange(range).setSend(Collections.singletonList(Collections.singletonList(body))).setValueInputOption(raw.length == 0 || !raw[0] ? "USER_ENTERED" : "RAW"));
        return this;
    }

    public RequestBuilder addAll(String range, List<List<Object>> body, boolean... raw) {
        //System.out.println("raw = " + Arrays.toString(raw));
        requests.add(new Request().setRange(range).setSend(body).setValueInputOption(raw.length == 0 || !raw[0] ? "USER_ENTERED" : "RAW"));
        return this;
    }

    public void execute() {
        System.out.println(requests.stream().map(r -> r.range).collect(Collectors.joining("\n")));
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
            if (!userentered.isEmpty())
                getSheetsService().spreadsheets().values().batchUpdate(sid, new BatchUpdateValuesRequest().setData(userentered).setValueInputOption("USER_ENTERED")).execute();
            if (!raw.isEmpty())
                getSheetsService().spreadsheets().values().batchUpdate(sid, new BatchUpdateValuesRequest().setData(raw).setValueInputOption("RAW")).execute();
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
