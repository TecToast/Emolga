package de.tectoast.emolga.ytsubscriber.impl;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndLink;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import de.tectoast.emolga.ytsubscriber.Subscriber;
import de.tectoast.emolga.ytsubscriber.Subscription;
import de.tectoast.emolga.ytsubscriber.SubscriptionHandler;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jetty.server.Server;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Basic {@link Subscriber} implementation. Uses Jetty as an internal webserver
 * for subscriber endpoints. Each {@link Subscription} is registered with an
 * auto-generated ID and verification token.
 *
 * @author Benjamin Erb
 */
public class SubscriberImpl implements Subscriber {
    private final int port;
    private final String host;
    private final Map<String, Subscription> subscriptions = new HashMap<>();
    private final Set<Map.Entry<URI, String>> subscribeIntents = new HashSet<>();
    private final Set<Map.Entry<URI, String>> unsubscribeIntents = new HashSet<>();
    private final ExecutorService executors = Executors.newFixedThreadPool(16);

    /**
     * Creates a new subscriber.
     *
     * @param host The external hostname of this subscriber
     * @param port The port this subscriber is listening to.
     */
    public SubscriberImpl(String host, int port) {
        this.host = host;
        this.port = port;
        Server server = new Server(port);
        SubscriptionHandler subscriptionHandler = new SubscriptionHandlerImpl(this);

        server.setHandler(subscriptionHandler);

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Subscription subscribe(URI feedTopicUri) {

        SyndFeedInput input = new SyndFeedInput();
        URI hubUri = null;
        SyndFeed feed;
        try {
            feed = input.build(new XmlReader(feedTopicUri.toURL()));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        for (Object s : feed.getLinks()) {
            if (s instanceof SyndLink) {
                SyndLink link = (SyndLink) s;
                if (link.getRel().equals("hub")) {
                    hubUri = URI.create(link.getHref());
                    break;
                }
            }
        }

        if (hubUri == null) {
            throw new IllegalArgumentException("Feed does not contain a hub relation");
        }

        Subscription subscription = new SubscriptionImpl(feedTopicUri, hubUri, this);

        //verify intent locally
        addSubscribeIntent(feedTopicUri, subscription.getVerifyToken());
        removeUnsubscribeIntent(subscription.getFeedTopicUri(), subscription.getVerifyToken());

        subscriptions.put(subscription.getInternalId(), subscription);

        //subscribe to hub
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(hubUri);
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");
        List<NameValuePair> params = new ArrayList<>();
        generateParams(params, subscription, SubscriptionMode.subscribe);
        try {
            post.setEntity(new UrlEncodedFormEntity(params));
            System.out.println(post);
            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() != 204) {
                throw new RuntimeException();
            }
        } catch (Exception e) {
            subscriptions.remove(subscription.getInternalId());
            post.abort();
            throw new RuntimeException(e);
        }
        return subscription;
    }

    private void generateParams(List<NameValuePair> params, Subscription subscription, SubscriptionMode mode) {
        params.add(new BasicNameValuePair("hub.callback", "http://" + host + ":" + port + "/" + subscription.getInternalId()));
        params.add(new BasicNameValuePair("hub.mode", mode.name()));
        params.add(new BasicNameValuePair("hub.topic", subscription.getFeedTopicUri().toString()));
        params.add(new BasicNameValuePair("hub.verify", "sync"));
        params.add(new BasicNameValuePair("hub.verify_token", subscription.getVerifyToken()));
    }

    @Override
    public void unsubscribe(Subscription subscription) {
        removeSubscribeIntent(subscription.getFeedTopicUri(), subscription.getVerifyToken());
        addUnsubscribeIntent(subscription.getFeedTopicUri(), subscription.getVerifyToken());

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(subscription.getHubUri());
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");
        List<NameValuePair> params = new ArrayList<>();
        generateParams(params, subscription, SubscriptionMode.unsubscribe);
        try {
            post.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response = client.execute(post);
            System.out.println(response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            subscriptions.remove(subscription.getInternalId());
            post.abort();
        } finally {
            subscriptions.remove(subscription.getInternalId());
        }
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public Subscription getSubscriptionById(String id) {
        return subscriptions.get(id);
    }

    @Override
    public void addSubscribeIntent(URI feedTopicUri, String verifyToken) {
        subscribeIntents.add(new AbstractMap.SimpleImmutableEntry<>(feedTopicUri, verifyToken));
    }

    @Override
    public boolean verifySubscribeIntent(URI feedTopicUri, String verifyToken) {
        return subscribeIntents.contains(new AbstractMap.SimpleImmutableEntry<>(feedTopicUri, verifyToken));
    }

    @Override
    public void removeSubscribeIntent(URI feedTopicUri, String verifyToken) {
        subscribeIntents.remove(new AbstractMap.SimpleImmutableEntry<>(feedTopicUri, verifyToken));
    }

    @Override
    public void addUnsubscribeIntent(URI feedTopicUri, String verifyToken) {
        unsubscribeIntents.add(new AbstractMap.SimpleImmutableEntry<>(feedTopicUri, verifyToken));
    }

    @Override
    public boolean verifyUnsubscribeIntent(URI feedTopicUri, String verifyToken) {
        return unsubscribeIntents.contains(new AbstractMap.SimpleImmutableEntry<>(feedTopicUri, verifyToken));
    }

    @Override
    public void removeUnsubscribeIntent(URI feedTopicUri, String verifyToken) {
        unsubscribeIntents.remove(new AbstractMap.SimpleImmutableEntry<>(feedTopicUri, verifyToken));
    }

    @Override
    public void executeCallback(Runnable runnable) {
        executors.execute(runnable);
    }

    @Override
    public String getHost() {
        return host;
    }

    enum SubscriptionMode {
        subscribe, unsubscribe
    }

}
