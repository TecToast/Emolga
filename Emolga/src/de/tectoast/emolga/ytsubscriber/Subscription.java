package de.tectoast.emolga.ytsubscriber;

import java.net.URI;

/**
 * The Subscription interface represents a subscription to a feed using a
 * distinct hub. Note that a {@link NotificationCallback} should be set in order
 * to react on incoming updates.
 * 
 * @author Benjamin Erb
 * 
 */
public interface Subscription
{

	String getInternalId();


	String getVerifyToken();


	URI getFeedTopicUri();


	URI getHubUri();


	Subscriber getSubscriber();


	void setNotificationCallback(NotificationCallback callback);


	NotificationCallback getNotificationCallback();
}
