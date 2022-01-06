package de.tectoast.emolga.bot;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.jetty.HttpHandler;
import de.tectoast.emolga.utils.Giveaway;
import de.tectoast.emolga.utils.MessageWaiter;
import de.tectoast.emolga.utils.sql.DBManagers;
import de.tectoast.toastilities.managers.ReactionManager;
import jakarta.xml.bind.DatatypeConverter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jsolf.JSONObject;

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
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Consumer;

import static de.tectoast.emolga.commands.Command.*;
import static de.tectoast.emolga.utils.Constants.MYSERVER;

public class EmolgaMain {

    public static final MessageWaiter messageWaiter = new MessageWaiter();
    public static final ArrayList<Giveaway> todel = new ArrayList<>();
    public static final ArrayList<String> alreadywritten = new ArrayList<>();
    public static final HashMap<String, Consumer<String>> sdmessages = new HashMap<>();
    public static JDA emolgajda;
    public static JDA flegmonjda;

    public static void start() throws Exception {
        emolgajda = JDABuilder.createDefault(Command.tokens.getString("discord"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .addEventListeners(new EmolgaListener(), messageWaiter)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();
        emolgajda.addEventListener(new SlashListener(emolgajda));
        flegmonjda = JDABuilder.createDefault(Command.tokens.getString("discordflegmon"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .addEventListeners(new EmolgaListener(), messageWaiter)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();
        flegmonjda.addEventListener(new SlashListener(emolgajda));
        /*ResultSet replana = Database.select("select * from analysis");
        while (replana.next()) {
            replayAnalysis.put(replana.getLong("replay"), replana.getLong("result"));
        }*/

        emolgajda.awaitReady();
        flegmonjda.awaitReady();
        //new EmolgaServer();
        setupJetty();
        Guild g = emolgajda.getGuildById(MYSERVER);
        g.updateCommands()
                .addCommands(commands.stream().filter(Command::isSlash).map(c -> {
                    CommandData dt = new CommandData(c.getName(), c.getHelp());
                    LinkedList<ArgumentManagerTemplate.Argument> args = c.getArgumentTemplate().arguments;
                    for (ArgumentManagerTemplate.Argument arg : args) {
                        dt.addOption(arg.getType().asOptionType(), arg.getName().toLowerCase(), arg.getHelp(), !arg.isOptional());
                    }
                    return dt;
                }).toArray(CommandData[]::new)).queue();
        //setupHerokuConnection();
        /*CommandListUpdateAction a = emolgajda.getGuildById(447357526997073930L).updateCommands();
        a.addCommands(commands.stream().filter(c -> c.getArgumentTemplate() != null).map(c -> {
            CommandData cd = new CommandData(c.getName(), c.getHelp());
            c.getArgumentTemplate().arguments.stream().sorted(Comparator.comparing(ArgumentManagerTemplate.Argument::isOptional)).forEach(ar -> cd.addOption(ar.getType().asOptionType(), ar.getName().toLowerCase().replace(" ", "_"), ar.getHelp(), !ar.isOptional()));
            return cd;
        }).collect(Collectors.toCollection(ArrayList::new))).queue();*/
        awaitNextDay();
        flegmonjda.getPresence().setActivity(Activity.playing("mit seiner Rute"));
        updatePresence();
        /*new RepeatTask(Instant.ofEpochMilli(1633888800000L), 6, Duration.ofDays(7), i -> {
            emolgajda.getTextChannelById(882641809531101256L).sendMessage("**-- Spieltag " + i + " --**").queue();
            emolgajda.getTextChannelById(882642106533949451L).sendMessage("**-- Spieltag " + i + " --**").queue();
        }, true);*/
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
        HashMap<Long, ScheduledFuture<?>> giveawayFutures = new HashMap<>();
        DBManagers.GIVEAWAY.forAll(r -> new Giveaway(r.getLong("channelid"), r.getLong("hostid"), r.getTimestamp("end").toInstant(), r.getInt("winners"), r.getString("prize"), r.getLong("messageid")));



        /*Subscription test = subscriber.subscribe(URI.create("https://www.youtube.com/xml/feeds/videos.xml?channel_id=UCypQULrVZkp1-_RUnrpJ2nQ"));
        test.setNotificationCallback(XML -> {
            logger.info("XML = " + XML);
        });*/
        //jda.getPresence().setActivity(Activity.playing("Wartungsarbeiten"));
    }

    private static void setupJetty() throws Exception {
        // Since this example shows off SSL configuration, we need a keystore
        // with the appropriate key. These lookup of jetty.home is purely a hack
        // to get access to a keystore that we use in many unit tests and should
        // probably be a direct path to your own keystore.

        // Create a basic jetty server object without declaring the port. Since
        // we are configuring connectors directly we'll be setting ports on
        // those connectors.
        Server server = new Server();

        // HTTP Configuration
        // HttpConfiguration is a collection of configuration information
        // appropriate for http and https. The default scheme for http is
        // <code>http</code> of course, as the default for secured http is
        // <code>https</code> but we show setting the scheme to show it can be
        // done. The port for secured communication is also set here.
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(51216);
        http_config.setOutputBufferSize(32768);

        // HTTP connector
        // The first server connector we create is the one for http, passing in
        // the http configuration we configured above so it can get things like
        // the output buffer size, etc. We also set the port (8080) and
        // configure an idle timeout.

        // SSL Context Factory for HTTPS
        // SSL requires a certificate so we configure a factory for ssl contents
        // with information pointing to what keystore the ssl connection needs
        // to know about. Much more configuration is available the ssl context,
        // including things like choosing the particular certificate out of a
        // keystore to be used.
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setSslContext(getContext());

        // HTTPS Configuration
        // A new HttpConfiguration object is needed for the next connector and
        // you can pass the old one as an argument to effectively clone the
        // contents. On this HttpConfiguration object we add a
        // SecureRequestCustomizer which is how a new connector is able to
        // resolve the https connection before handing control over to the Jetty
        // Server.
        HttpConfiguration https_config = new HttpConfiguration(http_config);
        SecureRequestCustomizer src = new SecureRequestCustomizer();
        src.setStsMaxAge(2000);
        src.setStsIncludeSubDomains(true);
        https_config.addCustomizer(src);

        // HTTPS connector
        // We create a second ServerConnector, passing in the http configuration
        // we just made along with the previously created ssl context factory.
        // Next we set the port and a longer idle timeout.
        ServerConnector https = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config));
        https.setPort(51216);
        https.setIdleTimeout(500000);

        // Here you see the server having multiple connectors registered with
        // it, now requests can flow into the server from both http and https
        // urls to their respective ports and be processed accordingly by jetty.
        // A simple handler is also registered with the server so the example
        // has something to pass requests off to.

        // Set the connectors
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

/*sdmessages.put("joinServer", str -> emolgajda.getTextChannelById("791284726677766155").sendMessage(str + " hat den Server betreten!").queue());
        sdmessages.put("leaveServer", str -> emolgajda.getTextChannelById("791284726677766155").sendMessage(str + " hat den Server verlassen!").queue());
        sdmessages.put("manualMessage", str -> {
            String user = str.split("\\|")[0];
            emolgajda.getTextChannelById("447357526997073932").sendMessage(user + ": " + str.substring(user.length() + 1)).queue();
        });*/

        /*new Thread(() -> {
            Path path = Paths.get("/home/florian/Showdown/sspserver/discord");
            try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
                final WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                while (true) {
                    final WatchKey wk = watchService.take();
                    for (WatchEvent<?> event : wk.pollEvents()) {
                        //we only register "ENTRY_MODIFY" so the context is always a Path.
                        final Path changed = (Path) event.context();
                        if (changed.endsWith("send.txt")) {
                            String message = String.join("\n", Files.readAllLines(path.resolve("send.txt")));
                            if (message.length() < 1) continue;
                            if (alreadywritten.remove(message)) continue;
                            alreadywritten.add(message);
                            logger.info("Message from SD: " + message);
                            String type = message.split("\\|")[0];
                            if (!sdmessages.containsKey(type)) {
                                sendToMe(type + " wurde noch nicht registriert!"); // Should never happen
                            } else {
                                sdmessages.get(type).accept(message.substring(type.length() + 1));
                            }
                        }
                    }
                    // reset the key
                    boolean valid = wk.reset();
                    if (!valid) {
                        logger.info("Key has been unregisterede");
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();*/
/*
new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                i++;
                //logger.info(Giveaway.toadd);
                Giveaway.giveaways.addAll(Giveaway.toadd);
                Giveaway.toadd.clear();
                Giveaway.giveaways.forEach(giveaway -> {
                    //logger.info("giveaway.messageId = " + giveaway.messageId);
                    if (giveaway.getMessageId() == -1) return;
                    if (giveaway.isEnded()) return;
                    try {
                        if (giveaway.getEnd().toEpochMilli() - System.currentTimeMillis() <= 10000 || i >= 5) {
                            //logger.info("giveaway.toString() = " + giveaway);
                            emolgajda.getTextChannelById(giveaway.getChannelId()).editMessageById(giveaway.getMessageId(), giveaway.render(Instant.now())).complete();
                        }
                    } catch (ErrorResponseException ex) {
                        ex.printStackTrace();
                        if (ex.getErrorCode() == 10008) {
                            sendToMe("GIVEAWAY DELETED!");
                            todel.add(giveaway);
                        }
                    }
                });
                if (i >= 5) i = 0;
                todel.forEach(Giveaway.giveaways::remove);
                boolean modified = false;
                for (Giveaway giveaway : todel) {
                    modified = true;
                    JSONObject json = getEmolgaJSON();
                    if (json.has("giveaways")) {
                        JSONArray arr = json.getJSONArray("giveaways");
                        int index = 0;
                        int x = -1;
                        for (Object o : arr) {
                            JSONObject obj = (JSONObject) o;
                            if (obj.getString("mid").equals(giveaway.messageId)) x = index;
                            index++;
                        }
                        if (x != -1) {
                            arr.remove(x);
                        }
                    }
                }
                todel.clear();
                if (modified) saveEmolgaJSON();
            }
        }, 0, 1000);
 */