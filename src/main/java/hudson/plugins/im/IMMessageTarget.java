package hudson.plugins.im;

import java.io.Serializable;

/**
 * A MessageTarget represents a user to send notifications to (like "peter@jabber.org"). 
 * @author Uwe Schaefer
 *
 */
// TODO: merge IMMessageTarget and IMChat interfaces - I doubt that we really need both
public interface IMMessageTarget extends Serializable
{

}
