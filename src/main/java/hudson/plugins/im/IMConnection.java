package hudson.plugins.im;


/**
 * Represents a connection to an IM-Server.
 * @author Uwe Schaefer
 */
public interface IMConnection
{
    /**
     * Establishes the connection and login.
     * 
     * @return <code>true</code> iff connection and login was successful
     */
    boolean connect();
    
    /**
     * Returns if the connection is established.
     */
    boolean isConnected();
    
    
    /**
     * Closes the connection (includes logout) and releases resources. 
     */
    void close();
    
    /**
     * Sends a Message-Text to an IMMessageTarget (aka a User ;).
     * @param target the target to send to 
     * @param text the text to be sent
     * @throws IMException
     */
    void send(IMMessageTarget target, String text) throws IMException;

    /**
     * Sets the current connection's presence to a protocol specific adaption of the given presence parameter.
     * May be ignored completely by some protocols.
     * 
     * @param presence the presence to set
     * @param statusMessage status message which should be deployed (maybe ignored by some protocols)
     * @throws IMException encapsulated exception of underlying protocol/client 
     */
    void setPresence(IMPresence presence, String statusMessage) throws IMException;

	void addConnectionListener(IMConnectionListener listener);

	void removeConnectionListener(IMConnectionListener listener);
}
