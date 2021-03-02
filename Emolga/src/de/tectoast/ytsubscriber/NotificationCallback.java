package de.tectoast.ytsubscriber;

import com.sun.syndication.feed.synd.SyndFeed;
import org.json.JSONObject;

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
	 * @param json feed
	 */
	void handle(JSONObject json);
}
