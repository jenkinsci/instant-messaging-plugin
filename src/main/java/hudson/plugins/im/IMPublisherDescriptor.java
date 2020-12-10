package hudson.plugins.im;

import hudson.plugins.im.config.ParameterNames;

import hudson.util.Secret;
import java.util.List;

public interface IMPublisherDescriptor {

    public static final String PARAMETERVALUE_STRATEGY_DEFAULT = NotificationStrategy.STATECHANGE_ONLY.getDisplayName();;
    public static final List<String> PARAMETERVALUE_STRATEGY_VALUES = NotificationStrategy.getDisplayNamesList();

    /**
     * Internally used to construct the parameter names on the config page.
     * @return a prefix which must be unique among all IM plugins.
     */
    public ParameterNames getParamNames();

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
     * @deprecated use {@link #getSecretPassword()}
     */
    @Deprecated
    default String getPassword() {
        throw new UnsupportedOperationException("use getSecretPassword()");
    }

    /**
     * Returns the password needed to login into the IM network.
     */
    Secret getSecretPassword();

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
