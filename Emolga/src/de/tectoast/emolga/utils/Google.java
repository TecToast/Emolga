package de.tectoast.emolga.utils;

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
import net.dv8tion.jda.annotations.ReplaceWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class Google {

    private static String REFRESHTOKEN;
    private static String CLIENTID;
    private static String CLIENTSECRET;
    private static String accesstoken;
    private static long lastUpdate = -1;

    public static void setCredentials(String refreshToken, String clientID, String clientSecret) {
        REFRESHTOKEN = refreshToken;
        CLIENTID = clientID;
        CLIENTSECRET = clientSecret;
    }


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
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("NULL");
        return null;
    }

    @Deprecated
    @ReplaceWith("RequestBuilder.updateAll")
    public static void updateRequest(String spreadsheetId, String range, List<List<Object>> values, boolean raw, boolean recursive) throws IllegalArgumentException {
        try {
            getSheetsService().spreadsheets().values().update(spreadsheetId, range, new ValueRange().setValues(values)).setValueInputOption(raw ? "RAW" : "USER_ENTERED").execute();
        } catch (GoogleJsonResponseException ex) {
            ex.printStackTrace();
            if (recursive) throw new IllegalArgumentException("Fehler bei updateRequest");
            generateAccessToken();
            updateRequest(spreadsheetId, range, values, raw, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Spreadsheet getSheetData(String spreadsheetId, boolean recursive, String... range) throws IllegalArgumentException {
        try {
            System.out.println(1);
            Sheets.Spreadsheets.Get get = getSheetsService().spreadsheets().get(spreadsheetId).setIncludeGridData(true);
            System.out.println(2);
            if (range != null) get.setRanges(Arrays.asList(range));
            System.out.println(3);
            return get.execute();
        } catch (GoogleJsonResponseException ex) {
            ex.printStackTrace();
            if (recursive) throw new IllegalArgumentException("Fehler bei getSheetData");
            generateAccessToken();
            return getSheetData(spreadsheetId, true, range);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static List<ValueRange> batchGet(String spreadsheetId, List<String> range, boolean formula, boolean recursive) throws IllegalArgumentException {
        try {
            return getSheetsService().spreadsheets().values().batchGet(spreadsheetId).setRanges(range).setValueRenderOption(formula ? "FORMULA" : "FORMATTED_VALUE").execute().getValueRanges();
        } catch (GoogleJsonResponseException ex) {
            ex.printStackTrace();
            if (recursive) throw new IllegalArgumentException("Fehler bei get");
            generateAccessToken();
            return batchGet(spreadsheetId, range, formula, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("NULL");
        return null;
    }

    @Deprecated
    public static void batchUpdateRequest(String spreadsheetId, Request request, boolean recursive) throws IllegalArgumentException {
        try {
            getSheetsService().spreadsheets().batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(request))).execute();
        } catch (GoogleJsonResponseException ex) {
            ex.printStackTrace();
            if (recursive) throw new IllegalArgumentException("Fehler bei batchUpdateRequest");
            generateAccessToken();
            batchUpdateRequest(spreadsheetId, request, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Sheets getSheetsService() {
        refreshTokenIfNotPresent();
        try {
            return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accesstoken))
                    .setApplicationName("emolga")
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static YouTube getYouTubeService() throws GeneralSecurityException, IOException {
        refreshTokenIfNotPresent();
        return new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accesstoken))
                .setApplicationName("emolga")
                .build();
    }

    public static void refreshTokenIfNotPresent() {
        if(accesstoken == null || System.currentTimeMillis() - lastUpdate > 3000000) generateAccessToken();
    }

    public static void generateAccessToken() {
        try {
            accesstoken = new GoogleRefreshTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), REFRESHTOKEN, CLIENTID, CLIENTSECRET).execute().getAccessToken();
            lastUpdate = System.currentTimeMillis();
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }


}
