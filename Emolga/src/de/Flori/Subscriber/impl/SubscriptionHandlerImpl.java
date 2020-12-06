package de.Flori.Subscriber.impl;


import de.Flori.Subscriber.Subscriber;
import de.Flori.Subscriber.Subscription;
import de.Flori.Subscriber.SubscriptionHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

/**
 * Basic {@link SubscriptionHandler} implementation. Incoming requests will be
 * checked and forwarded to the appropriate processing methods.
 *
 * @author Benjamin Erb
 */
public class SubscriptionHandlerImpl extends AbstractHandler implements SubscriptionHandler {
    private final Subscriber subscriber;

    public SubscriptionHandlerImpl(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void handleNotify(HttpServletRequest request, HttpServletResponse response, final Subscription subscription) {
        InputStream in = null;
        try {
            in = request.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String s;
        StringBuilder builder = new StringBuilder();
        try {
            while ((s = reader.readLine()) != null) {
                builder.append(s);
                //System.out.println("s = " + s);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //System.out.println("request.toString() = " + request.toString());
        try {
            //System.out.println(1);
            subscriber.executeCallback(() -> {
                if (subscription.getNotificationCallback() != null) {
                    subscription.getNotificationCallback().handle(builder.toString());
                    //System.out.println(2);
                } //else System.out.println(3);
            });
            //System.out.println(4);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            response.setStatus(200);
        }

    }

    @Override
    public void handleVerify(HttpServletRequest request, HttpServletResponse response, Subscription subscription) {
        if (request.getParameter("hub.mode") != null && request.getParameter("hub.topic") != null && request.getParameter("hub.challenge") != null && request.getParameter("hub.verify_token") != null) {
            URI feedTopicUri = URI.create(request.getParameter("hub.topic"));
            if (request.getParameter("hub.mode").equals("subscribe")) {
                if (subscriber.verifySubscribeIntent(feedTopicUri, request.getParameter("hub.verify_token"))) {
                    response.setStatus(200);
                    response.setContentType("text/plain");
                    try {
                        response.getWriter().write(request.getParameter("hub.challenge"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    response.setStatus(404);
                }
            } else if (request.getParameter("hub.mode").equals("unsubscribe")) {
                if (subscriber.verifyUnsubscribeIntent(feedTopicUri, request.getParameter("hub.verify_token"))) {
                    response.setStatus(200);
                    response.setContentType("text/plain");
                    try {
                        response.getWriter().write(request.getParameter("hub.challenge"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    response.setStatus(404);
                }
            }

        } else {
            response.setStatus(400);
        }

    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        Subscription subscription = subscriber.getSubscriptionById(target.substring(1));
        if (null != subscription) {
            if (request.getMethod().equals("GET")) {
                handleVerify(request, response, subscription);
            } else if (request.getMethod().equals("POST")) {
                handleNotify(request, response, subscription);
            } else {
                response.setStatus(405);
            }
            baseRequest.setHandled(true);
        }
    }

}
