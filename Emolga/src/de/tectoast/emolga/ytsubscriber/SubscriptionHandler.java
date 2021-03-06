package de.tectoast.emolga.ytsubscriber;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;

/**
 * A handler interface for subscriptions. This handler will be used for
 * processing incoming notifications by the hub.
 * 
 * @author Benjamin Erb
 * 
 */
public interface SubscriptionHandler extends Handler
{

	void handleVerify(HttpServletRequest request, HttpServletResponse response, Subscription subscription);



	void handleNotify(HttpServletRequest request, HttpServletResponse response, Subscription subscription);
}
