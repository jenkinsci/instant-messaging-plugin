package hudson.plugins.im;

public interface IMPublisherDescriptor {

	/**
	 * Returns <code>true</code> iff the plugin is globally enabled.
	 */
	boolean isEnabled();
	
    boolean isExposePresence();
    
    String getHost();
    
    /**
     * Returns the hostname. May be null in which case the host must be determined from the
     * Jabber 'service name'.
     * 
     * @deprecated Should be replaced by getHost
     */
    @Deprecated
    String getHostname();
    
    int getPort();
    
    /**
     * Returns the user name needed to login into the IM network.
     */
    String getUserName();
    
    /**
     * Returns the password needed to login into the IM network.
     */
    String getPassword();
    
    String getCommandPrefix();
 
    String getDefaultIdSuffix();
    
    /**
     * Returns the user name needed to login into Hudson.
     */
    String getHudsonUserName();
    
    /**
     * Returns the password needed to login into Hudson.
     */
    String getHudsonPassword();
}
