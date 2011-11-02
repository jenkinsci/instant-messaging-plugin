package hudson.plugins.im;

import java.util.List;

public interface IMPublisherDescriptor {

    static final String PREFIX = "im.";
    public static final String PARAMETERNAME_STRATEGY = PREFIX + "strategy";
    public static final String PARAMETERNAME_NOTIFY_START = PREFIX + "notifyStart";
    public static final String PARAMETERNAME_NOTIFY_SUSPECTS = PREFIX + "notifySuspects";
    public static final String PARAMETERNAME_NOTIFY_CULPRITS = PREFIX + "notifyCulprits";
    public static final String PARAMETERNAME_NOTIFY_FIXERS = PREFIX + "notifyFixers";
    public static final String PARAMETERNAME_NOTIFY_UPSTREAM_COMMITTERS = PREFIX + "notifyUpstreamCommitters";
    
    public static final String PARAMETERVALUE_STRATEGY_DEFAULT = NotificationStrategy.STATECHANGE_ONLY.getDisplayName();;
    public static final String[] PARAMETERVALUE_STRATEGY_VALUES = NotificationStrategy.getDisplayNames();
    public static final String PARAMETERNAME_HUDSON_LOGIN = PREFIX + "hudsonLogin";
    
	/**
	 * Returns <code>true</code> iff the plugin is globally enabled.
	 */
	boolean isEnabled();
	
	/**
	 * Returns an informal, short description of the concrete plugin.
	 */
	String getPluginDescription();
	
	/**
	 * Returns if the plugin should expose its presence on the IM network.
	 * I.e. if it should report as 'available' or that like.
	 */
    boolean isExposePresence();
    
    /**
     * Returns the hostname of the IM network. I.e. the host to which the plugin should connect.
     */
    String getHost();
    
    /**
     * Returns the hostname. May be null in which case the host must be determined from the
     * Jabber 'service name'.
     * 
     * @deprecated Should be replaced by getHost
     */
    @Deprecated
    String getHostname();
    
    /**
     * Returns the port of the IM network..
     */
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
     * Returns the default targets which should be used for build notification.
     * 
     * This can be overwritten on a per job basis.
     */
    List<IMMessageTarget> getDefaultTargets();
    
    IMMessageTargetConverter getIMMessageTargetConverter();
}
