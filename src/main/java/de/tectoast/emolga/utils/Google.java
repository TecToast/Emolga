package de.tectoast.emolga.utils;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import net.dv8tion.jda.annotations.ReplaceWith;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class Google {

    private static final Logger logger = LoggerFactory.getLogger(Google.class);

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


    public static @Nullable SearchResult getVidByQuery(String vid) throws IllegalArgumentException {
        try {
            return getYouTubeService().search().list(Collections.singletonList("snippet")).setQ(vid).setMaxResults((long) 1).execute().getItems().get(0);
        } catch (GeneralSecurityException | IOException ex) {
            ex.printStackTrace();
        }
        logger.info("NULL");
        return null;
    }

    public static @Nullable Video getVidByURL(String url) {
        try {
            String id = (url.contains("youtu.be") ? url.substring("https://youtu.be/".length()) : url.substring("https://www.youtube.com/watch?v=".length())).split("&")[0];
            return getYouTubeService().videos().list(Collections.singletonList("snippet")).setId(Collections.singletonList(id)).setMaxResults(1L).execute().getItems().get(0);
        } catch (GeneralSecurityException | IOException ex) {
            ex.printStackTrace();
        }
        logger.info("NULL");
        return null;
    }

    public static @Nullable Playlist getPlaylistByURL(String url) {
        try {
            String id = url.substring("https://www.youtube.com/playlist?list=".length()).split("&")[0];
            return getYouTubeService().playlists().list(Collections.singletonList("snippet")).setId(Collections.singletonList(id)).setMaxResults(1L).execute().getItems().get(0);
        } catch (GeneralSecurityException | IOException ex) {
            ex.printStackTrace();
        }
        logger.info("NULL");
        return null;
    }


    public static @Nullable List<List<Object>> get(String spreadsheetId, String range, boolean formula, boolean recursive) throws IllegalArgumentException {
        try {
            return getSheetsService().spreadsheets().values().get(spreadsheetId, range).setValueRenderOption(formula ? "FORMULA" : "FORMATTED_VALUE").execute().getValues();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        logger.info("NULL");
        return null;
    }

    @Deprecated
    @ReplaceWith("RequestBuilder.updateAll")
    public static void updateRequest(String spreadsheetId, String range, List<List<Object>> values, boolean raw, boolean recursive) throws IllegalArgumentException {
        try {
            getSheetsService().spreadsheets().values().update(spreadsheetId, range, new ValueRange().setValues(values)).setValueInputOption(raw ? "RAW" : "USER_ENTERED").execute();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static @Nullable Spreadsheet getSheetData(String spreadsheetId, boolean recursive, String... range) throws IllegalArgumentException {
        try {
            Sheets.Spreadsheets.Get get = getSheetsService().spreadsheets().get(spreadsheetId).setIncludeGridData(true);
            if (range != null) get.setRanges(Arrays.asList(range));
            return get.execute();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public static @Nullable List<ValueRange> batchGet(String spreadsheetId, List<String> range, boolean formula, boolean recursive) throws IllegalArgumentException {
        try {
            return getSheetsService().spreadsheets().values().batchGet(spreadsheetId).setRanges(range).setValueRenderOption(formula ? "FORMULA" : "FORMATTED_VALUE").execute().getValueRanges();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        logger.info("NULL");
        return null;
    }

    @Deprecated
    public static void batchUpdateRequest(String spreadsheetId, Request request, boolean recursive) throws IllegalArgumentException {
        try {
            getSheetsService().spreadsheets().batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(request))).execute();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static @Nullable Sheets getSheetsService() {
        refreshTokenIfNotPresent();
        try {
            return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accesstoken))
                    .setApplicationName("emolga")
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static YouTube getYouTubeService() throws GeneralSecurityException, IOException {
        refreshTokenIfNotPresent();
        return new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accesstoken))
                .setApplicationName("emolga")
                .build();
    }

    public static void refreshTokenIfNotPresent() {
        if (accesstoken == null || System.currentTimeMillis() - lastUpdate > 3000000) generateAccessToken();
    }

    public static void generateAccessToken() {
        try {
            accesstoken = new GoogleRefreshTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), REFRESHTOKEN, CLIENTID, CLIENTSECRET).execute().getAccessToken();
            lastUpdate = System.currentTimeMillis();
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }


}
