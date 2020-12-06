package de.Flori.Subscriber;

import java.net.URI;

/**
 * The subscriber interface. This interface is the basic interface of a
 * subscriber in the context of the PubSubHubBub protocol. This also includes an
 * embedded webserver for incoming notifications.
 *
 * It allows the subscription and unsubscription of feed URIs. Each subscription
 * creates a {@link Subscription} object, where a callback should be defined.
 *
 * @author Benjamin Erb
 *
 */

public interface Subscriber
{

    Subscription subscribe(URI feedTopicUri);

    void unsubscribe(Subscription subscription);

    int getPort();

    String getHost();

    Subscription getSubscriptionById(String id);

    boolean verifySubscribeIntent(URI feedTopicUri, String verifyToken);

    void addSubscribeIntent(URI feedTopicUri, String verifyToken);

    void removeSubscribeIntent(URI feedTopicUri, String verifyToken);

    boolean verifyUnsubscribeIntent(URI feedTopicUri, String verifyToken);

    void addUnsubscribeIntent(URI feedTopicUri, String verifyToken);

    void removeUnsubscribeIntent(URI feedTopicUri, String verifyToken);

    void executeCallback(Runnable runnable);
}
