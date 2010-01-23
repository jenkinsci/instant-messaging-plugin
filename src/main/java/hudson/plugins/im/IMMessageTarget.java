package hudson.plugins.im;

import java.io.Serializable;

/**
 * A MessageTarget represents a user to send notifications to (like "peter@jabber.org").
 * 
 * Usually, you don't need to implement this interface.
 * Instead, please use {@link DefaultIMMessageTarget} and {@link GroupChatIMMessageTarget}.
 * 
 * @author Uwe Schaefer
 */
public interface IMMessageTarget extends Serializable
{

}
