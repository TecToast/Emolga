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
    void handle(String xml);
}
