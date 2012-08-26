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
    
    public String getNotifyStart() {
        return getPrefix() + "notifyStart";
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

    
    