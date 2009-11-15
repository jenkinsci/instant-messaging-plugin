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
// TODO: merge IMMessageTarget and IMChat interfaces - I doubt that we really need both
public interface IMMessageTarget extends Serializable
{

}
