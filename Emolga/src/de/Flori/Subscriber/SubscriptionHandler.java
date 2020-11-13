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
@SuppressWarnings("JavaDoc")
public interface SubscriptionHandler extends Handler {
    /**
     * Process a verification request by the hub.
     *
     * @param request Request
     * @param response Response
     * @param subscription Subscription
     * @throws IOException
     * @throws ServletException
     */
    void handleVerify(HttpServletRequest request, HttpServletResponse response, Subscription subscription);

    /**
     * Process a notification request by the hub.
     *
     * @param request Request
     * @param response Response
     * @param subscription Subscription
     * @throws IOException
     * @throws ServletException
     */
    void handleNotify(HttpServletRequest request, HttpServletResponse response, Subscription subscription);
}
