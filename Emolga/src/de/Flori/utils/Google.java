package de.Flori.utils;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;


public class Google {

    public static String REFRESHTOKEN;
    public static String CLIENTID;
    public static String CLIENTSECRET;
    private static String accesstoken;

    public static SearchResult getVid(String vid, boolean recursive) throws IllegalArgumentException {
        try {
            return getYouTubeService().search().list(Collections.singletonList("snippet")).setQ(vid).setMaxResults((long) 1).execute().getItems().get(0);
        } catch (GoogleJsonResponseException ex) {
            ex.printStackTrace();
            if (recursive) throw new IllegalArgumentException("Fehler bei getVid");
            generateAccessToken();
            return getVid(vid, true);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        System.out.println("NULL");
        return null;
    }

    public static List<List<Object>> get(String spreadsheetId, String range, boolean formula, boolean recursive) throws IllegalArgumentException {
        try {
            return getSheetsService().spreadsheets().values().get(spreadsheetId, range).setValueRenderOption(formula ? "FORMULA" : "FORMATTED_VALUE").execute().getValues();
        } catch (GoogleJsonResponseException ex) {
            ex.printStackTrace();
            if (recursive) throw new IllegalArgumentException("Fehler bei get");
            generateAccessToken();
            return get(spreadsheetId, range, formula, true);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        System.out.println("NULL");
        return null;
    }

    public static void updateRequest(String spreadsheetId, String range, List<List<Object>> values, boolean raw, boolean recursive) throws IllegalArgumentException {
        try {
            getSheetsService().spreadsheets().values().update(spreadsheetId, range, new ValueRange().setValues(values)).setValueInputOption(raw ? "RAW" : "USER_ENTERED").execute();
        } catch (GoogleJsonResponseException ex) {
            ex.printStackTrace();
            if (recursive) throw new IllegalArgumentException("Fehler bei updateRequest");
            generateAccessToken();
            updateRequest(spreadsheetId, range, values, raw, true);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public static Spreadsheet getSheetData(String spreadsheetId, String range, boolean recursive) throws IllegalArgumentException {
        try {
            return getSheetsService().spreadsheets().get(spreadsheetId).setIncludeGridData(true).setRanges(Collections.singletonList(range)).execute();
        } catch (GoogleJsonResponseException ex) {
            ex.printStackTrace();
            if (recursive) throw new IllegalArgumentException("Fehler bei getSheetData");
            generateAccessToken();
            return getSheetData(spreadsheetId, range, true);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void batchUpdateRequest(String spreadsheetId, Request request, boolean recursive) throws IllegalArgumentException {
        try {
            getSheetsService().spreadsheets().batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(request))).execute();
        } catch (GoogleJsonResponseException ex) {
            ex.printStackTrace();
            if (recursive) throw new IllegalArgumentException("Fehler bei batchUpdateRequest");
            generateAccessToken();
            batchUpdateRequest(spreadsheetId, request, true);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private static Sheets getSheetsService() throws GeneralSecurityException, IOException {
        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accesstoken))
                .setApplicationName("Emolga")
                .build();
    }

    private static YouTube getYouTubeService() throws GeneralSecurityException, IOException {
        return new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accesstoken))
                .setApplicationName("Emolga")
                .build();
    }

    private static void generateAccessToken() {
        try {
            accesstoken = new GoogleRefreshTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), REFRESHTOKEN, CLIENTID, CLIENTSECRET).execute().getAccessToken();
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }


}
