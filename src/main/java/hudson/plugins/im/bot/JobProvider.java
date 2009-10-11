package hudson.plugins.im.bot;

import hudson.model.AbstractProject;

public interface JobProvider {
    /**
     * Returns the Hudson job with the given name or null
     * if no job with that name exists.
     */
    AbstractProject<?, ?> getJobByName(String name);
}
