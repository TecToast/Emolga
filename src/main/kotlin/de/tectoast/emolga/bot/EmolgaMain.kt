package de.tectoast.emolga.bot

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.jetty.HttpHandler
import de.tectoast.emolga.utils.Giveaway
import de.tectoast.emolga.utils.sql.managers.GiveawayManager
import jakarta.xml.bind.DatatypeConverter
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.*
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.security.KeyFactory
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.sql.ResultSet
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

object EmolgaMain {

    lateinit var emolgajda: JDA
    lateinit var flegmonjda: JDA
    private val logger = LoggerFactory.getLogger(EmolgaMain::class.java)

    @Throws(Exception::class)
    fun start() {
        emolgajda = JDABuilder.createDefault(Command.tokens.getString("discord"))
            .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
            .addEventListeners(EmolgaListener(), SlashListener())
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .build()
        flegmonjda = JDABuilder.createDefault(Command.tokens.getString("discordflegmon"))
            .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
            .addEventListeners(EmolgaListener(), SlashListener())
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .build()
        emolgajda.awaitReady()
        flegmonjda.awaitReady()
        logger.info("Discord Bots loaded!")
        setupJetty()
        Command.awaitNextDay()
        flegmonjda.presence.activity = Activity.playing("mit seiner Rute")
        Command.updatePresence()
        /*val manager = ReactionManager(emolgajda)
        manager // BS
            .registerReaction("827608009571958806", "884567614918111233", "884564674744561684", "884565654227812364")
            .registerReaction("827608009571958806", "884567614918111233", "884564533295869962", "884565697479458826")
            .registerReaction("827608009571958806", "884567614918111233", "884565288564195348", "884565609663320086")
            .registerReaction("827608009571958806", "884567614918111233", "886748333484441650", "886746672120606771")
            .registerReaction("827608009571958806", "884567614918111233", "886748333484441650", "886746672120606771")
            .registerReaction("827608009571958806", "884567614918111233", "921389285188440115", "921387730200584222")*/
        GiveawayManager.forAll { r: ResultSet ->
            Giveaway(
                r.getLong("channelid"),
                r.getLong("hostid"),
                r.getTimestamp("end").toInstant(),
                r.getInt("winners"),
                r.getString("prize"),
                r.getLong("messageid")
            )
        }
    }

    @Throws(Exception::class)
    private fun setupJetty() {
        val server = Server()
        val httpConfig = HttpConfiguration()
        httpConfig.secureScheme = "https"
        httpConfig.securePort = 51216
        httpConfig.outputBufferSize = 32768
        val sslContextFactory = SslContextFactory.Server()
        sslContextFactory.sslContext = context
        val httpsConfig = HttpConfiguration(httpConfig)
        val src = SecureRequestCustomizer()
        src.stsMaxAge = 2000
        src.isStsIncludeSubDomains = true
        httpsConfig.addCustomizer(src)
        val https = ServerConnector(
            server,
            SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
            HttpConnectionFactory(httpsConfig)
        )
        https.port = 51216
        https.idleTimeout = 500000
        server.connectors = arrayOf<Connector>(https)
        server.handler = HttpHandler()
        server.start()
    }

    private val context: SSLContext
        get() {
            val tokens = Command.tokens.getJSONObject("website")
            val password = tokens.getString("password")
            val pathname = tokens.getString("path")
            val context: SSLContext = SSLContext.getInstance("TLS")
            val certBytes = parseDERFromPEM(
                getBytes(File(pathname + File.separator + "cert.pem")),
                "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----"
            )
            val keyBytes = parseDERFromPEM(
                getBytes(File(pathname + File.separator + "privkey.pem")),
                "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----"
            )
            val cert = generateCertificateFromDER(certBytes)
            val key = generatePrivateKeyFromDER(keyBytes)
            val keystore = KeyStore.getInstance("JKS")
            keystore.load(null)
            keystore.setCertificateEntry("cert-alias", cert)
            keystore.setKeyEntry("key-alias", key, password.toCharArray(), arrayOf<Certificate>(cert))
            val kmf = KeyManagerFactory.getInstance("SunX509")
            kmf.init(keystore, password.toCharArray())
            val km = kmf.keyManagers
            context.init(km, null, null)
            return context
        }

    private fun parseDERFromPEM(pem: ByteArray, beginDelimiter: String, endDelimiter: String): ByteArray {
        val data = String(pem)
        var tokens = data.split(beginDelimiter.toRegex())
        tokens = tokens[1].split(endDelimiter.toRegex())
        return DatatypeConverter.parseBase64Binary(tokens[0])
    }

    @Throws(InvalidKeySpecException::class, NoSuchAlgorithmException::class)
    private fun generatePrivateKeyFromDER(keyBytes: ByteArray): RSAPrivateKey {
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val factory = KeyFactory.getInstance("RSA")
        return factory.generatePrivate(spec) as RSAPrivateKey
    }

    @Throws(CertificateException::class)
    private fun generateCertificateFromDER(certBytes: ByteArray): X509Certificate {
        val factory = CertificateFactory.getInstance("X.509")
        return factory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
    }

    private fun getBytes(file: File): ByteArray {
        return Files.readAllBytes(file.toPath())
    }
}