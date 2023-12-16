package de.tectoast.emolga.utils

import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import com.google.api.services.sheets.v4.Sheets

object Google {
    private var REFRESHTOKEN: String? = null
    private var CLIENTID: String? = null
    private var CLIENTSECRET: String? = null
    private var accesstoken: String? = null
    private var lastUpdate: Long = -1
    fun setCredentials(refreshToken: String?, clientID: String?, clientSecret: String?) {
        REFRESHTOKEN = refreshToken
        CLIENTID = clientID
        CLIENTSECRET = clientSecret
    }

    operator fun get(spreadsheetId: String, range: String, formula: Boolean): List<List<Any>> {
        return sheetsService.spreadsheets()
            .values()[spreadsheetId, range].setValueRenderOption(if (formula) "FORMULA" else "FORMATTED_VALUE")
            .execute().getValues()
    }

    fun batchGet(sid: String, ranges: List<String>, formula: Boolean, majorDimension: String = "ROWS") =
        sheetsService.spreadsheets().values().batchGet(sid).setRanges(ranges).setMajorDimension(majorDimension)
            .setValueRenderOption(if (formula) "FORMULA" else "FORMATTED_VALUE")
            .execute().valueRanges.map { it.getValues() }

    fun uploadFileToDrive(parent: String, name: String, mimeType: String, data: ByteArray): String {
        val fileId = driveService.files().create(
            File().setParents(listOf(parent)).setName(name),
            ByteArrayContent(
                mimeType,
                data
            )
        ).setUploadType("media").setUseContentAsIndexableText(false).execute().id
        driveService.permissions().create(fileId, Permission().setType("anyone").setRole("reader")).execute()
        return fileId
    }

    val sheetsService: Sheets
        get() {
            refreshTokenIfNotPresent()
            return Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(
                    accesstoken
                )
            ).setApplicationName("emolga").build()
        }

    val driveService: Drive
        get() {
            refreshTokenIfNotPresent()
            return Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(
                    accesstoken
                )
            )
                .setApplicationName("emolga")
                .build()
        }

    private fun refreshTokenIfNotPresent() {
        if (accesstoken == null || System.currentTimeMillis() - lastUpdate > 3000000) generateAccessToken()
    }

    fun generateAccessToken() {
        accesstoken = GoogleRefreshTokenRequest(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            REFRESHTOKEN,
            CLIENTID,
            CLIENTSECRET
        ).execute().accessToken
        lastUpdate = System.currentTimeMillis()
    }
}
