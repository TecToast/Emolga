package de.Flori.Subscriber;

import org.eclipse.jetty.server.Handler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A handler interface for subscriptions. This handler will be used for
 * processing incoming notifications by the hub.
 *
 * @author Benjamin Erb
 */

public interface SubscriptionHandler extends Handler {

    void handleVerify(HttpServletRequest request, HttpServletResponse response, Subscription subscription);

    void handleNotify(HttpServletRequest request, HttpServletResponse response, Subscription subscription);
}
