package de.Flori.Subscriber;

import com.sun.syndication.feed.synd.SyndFeed;


/**
 * This interface provides a callback method that handles incoming feed updates.
 *
 * @author Benjamin Erb
 *
 */
public interface NotificationCallback
{
    /**
     * The handle method is executed each time the original feed provides
     * updates. The parameter contains a {@link SyndFeed} with all new entries.
     *
     * @param xml The XML String
     */
    void handle(String xml);
}
