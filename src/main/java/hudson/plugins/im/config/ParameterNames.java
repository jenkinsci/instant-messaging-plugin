package hudson.plugins.im.config;

/**
 * Parameter names for the configuration pages.
 *
 * @author kutzi
 */
public abstract class ParameterNames {

    protected abstract String getPrefix();

    public String getStrategy() {
        return getPrefix() + "strategy";
    }

    // Note: A pipeline step equivalent would be 'notifyOnStart'
    public String getNotifyStart() {
        return getPrefix() + "notifyStart";
    }

    public String getExtraMessage() {
        return getPrefix() + "extraMessage";
    }

    // Note: This is not currently exposed in freestyle jobs,
    // but was added here for consistency
    public String getCustomMessage() {
        return getPrefix() + "customMessage";
    }

    public String getNotifySuspects() {
        return getPrefix() + "notifySuspects";
    }

    public String getNotifyCulprits() {
        return getPrefix() + "notifyCulprits";
    }

    public String getNotifyFixers() {
        return getPrefix() + "notifyFixers";
    }
    public String getNotifyUpstreamCommitters() {
        return getPrefix() + "notifyUpstreamCommitters";
    }

    public String getJenkinsLogin() {
        return getPrefix() + "jenkinsLogin";
    }
}
