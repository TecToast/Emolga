package de.tectoast.emolga.bot

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.database.exposed.Giveaway
import de.tectoast.emolga.jetty.HttpHandler
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.Emolga
import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.jdabuilder.default
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.messages.reply_
import jakarta.xml.bind.DatatypeConverter
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
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
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

object EmolgaMain {

    lateinit var emolgajda: JDA
    lateinit var flegmonjda: JDA
    private val logger = LoggerFactory.getLogger(EmolgaMain::class.java)

    @Throws(Exception::class)
    fun start() {
        emolgajda = default(Command.tokens.getString("discord")) {
            //intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
            intents -= GatewayIntent.MESSAGE_CONTENT
            addEventListeners(EmolgaListener)
            setMemberCachePolicy(MemberCachePolicy.DEFAULT)
        }
        flegmonjda = default(Command.tokens.getString("discordflegmon")) {
            intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
            addEventListeners(EmolgaListener)
            setMemberCachePolicy(MemberCachePolicy.ALL)
        }
        initializeASLS11(emolgajda)
        EmolgaListener.registerEvents(emolgajda)
        EmolgaListener.registerEvents(flegmonjda)
        emolgajda.awaitReady()
        flegmonjda.awaitReady()
        logger.info("Discord Bots loaded!")
        //Ktor.start()
        Command.awaitNextDay()
        flegmonjda.presence.activity = Activity.playing("mit seiner Rute")
        Command.updatePresence()/*val manager = ReactionManager(emolgajda)
        manager // BS
            .registerReaction("827608009571958806", "884567614918111233", "884564674744561684", "884565654227812364")
            .registerReaction("827608009571958806", "884567614918111233", "884564533295869962", "884565697479458826")
            .registerReaction("827608009571958806", "884567614918111233", "884565288564195348", "884565609663320086")
            .registerReaction("827608009571958806", "884567614918111233", "886748333484441650", "886746672120606771")
            .registerReaction("827608009571958806", "884567614918111233", "886748333484441650", "886746672120606771")
            .registerReaction("827608009571958806", "884567614918111233", "921389285188440115", "921387730200584222")*/
        /*GiveawayManager.forAll {
            Giveaway(
                it.getLong("channelid"),
                it.getLong("hostid"),
                it.getTimestamp("end").toInstant(),
                it.getInt("winners"),
                it.getString("prize"),
                it.getLong("messageid")
            )
        }*/
        Giveaway.init()
    }

    private fun initializeASLS11(jda: JDA) {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { _, t ->
            logger.error("ERROR IN ASL SCOPE", t)
            Command.sendToMe("Error in asl scope, look in console")
        })
        jda.listener<SlashCommandInteractionEvent> { e ->
            if (e.name != "bet") return@listener
            Emolga.get.asls11.textChannel.let {
                if (e.channel.idLong != it) {
                    e.reply_("Dieser Command funktioniert nur im Channel <#$it>!", ephemeral = true).queue()
                    return@listener
                }
            }
            val startbet = e.getOption("startbet")!!.asInt
            if (!startbet.validBet()) {
                e.reply_("Das ist kein gültiges Startgebot!", ephemeral = true).queue()
                return@listener
            }
            val slashUserId = e.user.idLong.let { if (it == Constants.FLOID) Emolga.get.asls11.currentCoach else it }
            val steamdata = (Emolga.get.asls11.teamByCoach(slashUserId) ?: run {
                e.reply_("Du bist tatsächlich kein Coach c:", ephemeral = true).queue()
                return@listener
            })
            if (slashUserId != Emolga.get.asls11.currentCoach) {
                e.reply_("Du bist nicht dran!", ephemeral = true).queue()
                return@listener
            }

            steamdata.pointsToSpend().let {
                if (startbet > it) {
                    e.reply_("Du kannst maximal mit $it Punkten bieten!", ephemeral = true).queue()
                    return@listener
                }
            }
            val togain = e.getOption("player")!!.asMember!!
            if (!Emolga.get.asls11.isPlayer(togain)) {
                e.reply_("Dieser Trainer nimmt an dieser Season nicht als Teilnehmer teil!", ephemeral = true).queue()
                return@listener
            }
            if (Emolga.get.asls11.isTaken(togain.idLong)) {
                e.reply_("Dieser Trainer ist bereits verkauft!", ephemeral = true).queue()
                return@listener
            }
            val level = Emolga.get.asls11.getLevelByMember(togain).also {
                if (it in steamdata.members) {
                    e.reply_("Du hast bereits jemanden in Stufe $it!", ephemeral = true).queue()
                    return@listener
                }
            }
            e.reply(
                "${e.user.asMention} hat ${togain.asMention} (**Stufe $level**) für **$startbet Punkte** in den Ring geworfen!\n" + "Lasset das Versteigern beginnen!"
            ).queue()
            var maxBet: Pair<Long, Int> = slashUserId to startbet
            val countdown = AtomicInteger(Emolga.get.asls11.config.countdownSeconds)
            var countdownJob: Job? = null
            var finished = false
            var alreadyLaunched = false
            while (!finished) {
                val res = withTimeoutOrNull(Emolga.get.asls11.config.waitFor) {
                    val me: MessageReceivedEvent
                    var newbet: Int = -1
                    me = jda.await { event ->
                        if (event.author.isBot || event.channel.idLong != e.channel.idLong) return@await false
                        val t = Emolga.get.asls11.teamByCoach(event.author.idLong)
                        val nbet = event.message.contentDisplay.toIntOrNull() ?: -1
                        if (t == null || !nbet.validBet() || nbet <= maxBet.second || (t.pointsToSpend() < nbet).also {
                                if (it) Command.sendToUser(event.author, "So viel kannst du nicht mehr bieten!")
                            } || (level in t.members).also {
                                if (it) Command.sendToUser(
                                    event.author,
                                    "Du kannst hier nicht mitbieten, da du bereits einen Sklaven aus Stufe $level hast, du Kek! (Henny wollte, dass ich das so schreibe)"
                                )
                            }) {
                            event.message.delete().queue()
                            return@await false
                        }
                        newbet = nbet
                        true
                        //&& event.member!!.roles.any { it.idLong == 998164505529950258 }
                    }
                    if (!finished) {
                        countdownJob?.cancel()
                        alreadyLaunched = false
                        countdown.set(Emolga.get.asls11.config.countdownSeconds)
                        maxBet = me.author.idLong to newbet
                    }
                }
                logger.info("WithTimeout returns $res")

                if (res == null && !alreadyLaunched) {
                    alreadyLaunched = true
                    countdownJob = scope.launch {
                        while (countdown.get() > 0) {
                            val get = countdown.getAndDecrement()
                            if (get in Emolga.get.asls11.config.sendOn) e.channel.sendMessage("$get Sekunde${if (get != 1) "n" else ""}...")
                                .queue()
                            delay(1000)
                        }
                        finished = true
                        e.channel.sendMessage("${togain.asMention} gehört jetzt <@${maxBet.first}>, welcher für **${maxBet.second} Punkte** einen neuen Menschen versklavt hat!")
                            .queue()
                        Emolga.get.asls11.addUserToTeam(togain, maxBet.first, maxBet.second)
                        saveEmolgaJSON()
                        delay(5000)
                        Emolga.get.asls11.nextCoach()
                    }
                }
            }
        }
    }

    private fun Int.validBet() = this > 0 && this % 100 == 0

    @Throws(Exception::class)
    private fun setupJetty() {
        val server = Server()
        val httpConfig = HttpConfiguration().apply {
            secureScheme = "https"
            securePort = 51216
            outputBufferSize = 32768
        }
        val sslContextFactory = SslContextFactory.Server().apply {
            sslContext = context
        }
        val httpsConfig = HttpConfiguration(httpConfig)
        val src = SecureRequestCustomizer().apply {
            stsMaxAge = 2000
            isStsIncludeSubDomains = true
        }
        httpsConfig.addCustomizer(src)
        val https = ServerConnector(
            server,
            SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
            HttpConnectionFactory(httpsConfig)
        ).apply {
            port = 51216
            idleTimeout = 500000
        }
        server.apply {
            connectors = arrayOf<Connector>(https)
            handler = HttpHandler
            start()
        }
    }

    private val context: SSLContext
        get() {
            val tokens = Command.tokens.getJSONObject("website")
            val password = tokens.getString("password")
            val pathname = tokens.getString("path")
            val context: SSLContext = SSLContext.getInstance("TLS")
            val certBytes = parseDERFromPEM(
                getBytes(File(pathname + File.separator + "cert.pem")),
                "-----BEGIN CERTIFICATE-----",
                "-----END CERTIFICATE-----"
            )
            val keyBytes = parseDERFromPEM(
                getBytes(File(pathname + File.separator + "privkey.pem")),
                "-----BEGIN PRIVATE KEY-----",
                "-----END PRIVATE KEY-----"
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
        var tokens = data.split(beginDelimiter)
        tokens = tokens[1].split(endDelimiter)
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
