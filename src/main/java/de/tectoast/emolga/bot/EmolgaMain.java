package de.tectoast.emolga.bot;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.jetty.HttpHandler;
import de.tectoast.emolga.utils.Giveaway;
import de.tectoast.emolga.utils.sql.DBManagers;
import de.tectoast.jsolf.JSONObject;
import de.tectoast.toastilities.managers.ReactionManager;
import jakarta.xml.bind.DatatypeConverter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Function;

import static de.tectoast.emolga.commands.Command.*;
import static de.tectoast.emolga.utils.Constants.MYSERVER;

public class EmolgaMain {

    public static final ArrayList<Giveaway> todel = new ArrayList<>();
    public static final ArrayList<String> alreadywritten = new ArrayList<>();
    public static final HashMap<String, Consumer<String>> sdmessages = new HashMap<>();
    public static JDA emolgajda;
    public static JDA flegmonjda;

    public static void start() throws Exception {
        emolgajda = JDABuilder.createDefault(Command.tokens.getString("discord"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .addEventListeners(new EmolgaListener())
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();
        emolgajda.addEventListener(new SlashListener(emolgajda));
        flegmonjda = JDABuilder.createDefault(Command.tokens.getString("discordflegmon"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .addEventListeners(new EmolgaListener())
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();
        flegmonjda.addEventListener(new SlashListener(emolgajda));
        emolgajda.awaitReady();
        flegmonjda.awaitReady();
        setupJetty();
        Guild g = emolgajda.getGuildById(MYSERVER);
        Function<Command, SlashCommandData> cmdmapper = c -> {
            SlashCommandData dt = Commands.slash(c.getName(), c.getHelp());
            List<ArgumentManagerTemplate.Argument> args = c.getArgumentTemplate().arguments;
            for (ArgumentManagerTemplate.Argument arg : args) {
                dt.addOption(arg.getType().asOptionType(), arg.getName().toLowerCase(), arg.getHelp(), !arg.isOptional());
            }
            return dt;
        };
        g.updateCommands().addCommands(commands.values().stream().filter(Command::isSlash).filter(c -> c.getCategory() != CommandCategory.Soullink).map(cmdmapper).toArray(CommandData[]::new))
                .queue();
        emolgajda.getGuildById(695943416789598208L).updateCommands()
                .addCommands(commands.values().stream().filter(Command::isSlash).filter(c -> c.getCategory() == CommandCategory.Soullink).map(cmdmapper).toArray(CommandData[]::new)).queue();


        awaitNextDay();
        flegmonjda.getPresence().setActivity(Activity.playing("mit seiner Rute"));
        updatePresence();
        ReactionManager manager = new ReactionManager(emolgajda);
        manager
                // BS
                .registerReaction("715249205186265178", "813025531779743774", "813025179114405898", "719928482544484352")
                .registerReaction("715249205186265178", "813025531779743774", "813025403098628097", "813005659619590184")
                .registerReaction("715249205186265178", "813025531779743774", "813025709232488480", "813027599743713320")
                // ASL Minecraft
                .registerReaction("540899923789611018", "820784528888561715", "820781668586618901", "820783085976420372")
                // ASL D&D
                .registerReaction("830146866812420116", "830391184459300915", "540969934457667613", "830392346348355594")
                // Müslistrikers
                .registerReaction("827608009571958806", "884567614918111233", "884564674744561684", "884565654227812364")
                .registerReaction("827608009571958806", "884567614918111233", "884564533295869962", "884565697479458826")
                .registerReaction("827608009571958806", "884567614918111233", "884565288564195348", "884565609663320086")
                .registerReaction("827608009571958806", "884567614918111233", "886748333484441650", "886746672120606771")
                .registerReaction("827608009571958806", "884567614918111233", "886748333484441650", "886746672120606771")
                .registerReaction("827608009571958806", "884567614918111233", "921389285188440115", "921387730200584222");
        ScheduledExecutorService giveawayscheduler = new ScheduledThreadPoolExecutor(5);
        Map<Long, ScheduledFuture<?>> giveawayFutures = new HashMap<>();
        DBManagers.GIVEAWAY.forAll(r -> new Giveaway(r.getLong("channelid"), r.getLong("hostid"), r.getTimestamp("end").toInstant(), r.getInt("winners"), r.getString("prize"), r.getLong("messageid")));
    }

    private static void setupJetty() throws Exception {
        Server server = new Server();
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(51216);
        http_config.setOutputBufferSize(32768);
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setSslContext(getContext());
        HttpConfiguration https_config = new HttpConfiguration(http_config);
        SecureRequestCustomizer src = new SecureRequestCustomizer();
        src.setStsMaxAge(2000);
        src.setStsIncludeSubDomains(true);
        https_config.addCustomizer(src);
        ServerConnector https = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config));
        https.setPort(51216);
        https.setIdleTimeout(500000);
        server.setConnectors(new Connector[]{https});
        server.setHandler(new HttpHandler());
        server.start();
    }

    private static SSLContext getContext() {
        SSLContext context;
        JSONObject tokens = Command.tokens.getJSONObject("website");
        String password = tokens.getString("password");
        String pathname = tokens.getString("path");
        try {
            context = SSLContext.getInstance("TLS");

            byte[] certBytes = parseDERFromPEM(getBytes(new File(pathname + File.separator + "cert.pem")),
                    "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
            byte[] keyBytes = parseDERFromPEM(
                    getBytes(new File(pathname + File.separator + "privkey.pem")),
                    "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");

            X509Certificate cert = generateCertificateFromDER(certBytes);
            RSAPrivateKey key = generatePrivateKeyFromDER(keyBytes);

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            keystore.setCertificateEntry("cert-alias", cert);
            keystore.setKeyEntry("key-alias", key, password.toCharArray(), new Certificate[]{cert});

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, password.toCharArray());

            KeyManager[] km = kmf.getKeyManagers();

            context.init(km, null, null);
        } catch (Exception e) {
            context = null;
        }
        return context;
    }

    private static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] tokens = data.split(beginDelimiter);
        tokens = tokens[1].split(endDelimiter);
        return DatatypeConverter.parseBase64Binary(tokens[0]);
    }

    private static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey) factory.generatePrivate(spec);
    }

    private static X509Certificate generateCertificateFromDER(byte[] certBytes)
            throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    private static byte[] getBytes(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}