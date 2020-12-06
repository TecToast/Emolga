package de.tectoast.ytsubscriber;


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
