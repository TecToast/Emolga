package de.tectoast.ytsubscriber.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import de.tectoast.ytsubscriber.Subscriber;
import de.tectoast.ytsubscriber.Subscription;
import de.tectoast.ytsubscriber.SubscriptionHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;
import org.json.XML;

/**
 * Basic {@link SubscriptionHandler} implementation. Incoming requests will be
 * checked and forwarded to the appropriate processing methods.
 * 
 * @author Benjamin Erb
 * 
 */
public class SubscriptionHandlerImpl extends AbstractHandler implements SubscriptionHandler
{
	private final Subscriber subscriber;

	/**
	 * Creates a new {@link SubscriptionHandler}.
	 * @param subscriber the subscriber
	 */
	public SubscriptionHandlerImpl(Subscriber subscriber)
	{
		this.subscriber = subscriber;
	}

	@Override
	public void handleNotify(HttpServletRequest request, HttpServletResponse response, final Subscription subscription) {
		try
		{
			JSONObject json = XML.toJSONObject(new InputStreamReader(request.getInputStream()));
			subscriber.executeCallback(() -> {
				if(subscription.getNotificationCallback() != null)
				{
					subscription.getNotificationCallback().handle(json);
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			response.setStatus(200);
		}

	}

	@Override
	public void handleVerify(HttpServletRequest request, HttpServletResponse response, Subscription subscription)
	{
		if(request.getParameter("hub.mode") != null && request.getParameter("hub.topic") != null && request.getParameter("hub.challenge") != null && request.getParameter("hub.verify_token") != null)
		{
			URI feedTopicUri = URI.create(request.getParameter("hub.topic"));
			if(request.getParameter("hub.mode").equals("subscribe"))
			{
				if(subscriber.verifySubscribeIntent(feedTopicUri, request.getParameter("hub.verify_token")))
				{
					response.setStatus(200);
					response.setContentType("text/plain");
					try
					{
						response.getWriter().write(request.getParameter("hub.challenge"));
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
				else
				{
					response.setStatus(404);
				}
			}
			else if(request.getParameter("hub.mode").equals("unsubscribe"))
			{
				if(subscriber.verifyUnsubscribeIntent(feedTopicUri, request.getParameter("hub.verify_token")))
				{
					response.setStatus(200);
					response.setContentType("text/plain");
					try
					{
						response.getWriter().write(request.getParameter("hub.challenge"));
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
				else
				{
					response.setStatus(404);
				}
			}

		}
		else
		{
			response.setStatus(400);
		}

	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
		Subscription subscription = subscriber.getSubscriptionById(target.substring(1));
		if(null != subscription)
		{
			if(request.getMethod().equals("GET"))
			{
				handleVerify(request, response, subscription);
			}
			else if(request.getMethod().equals("POST"))
			{
				handleNotify(request, response, subscription);
			}
			else
			{
				response.setStatus(405);
			}
			baseRequest.setHandled(true);
		}
	}

}
