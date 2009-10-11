package hudson.plugins.im.bot;

import hudson.model.AbstractProject;
import hudson.model.View;

import java.util.List;

public interface JobProvider {
    /**
     * Returns the Hudson job with the given name or null
     * if no job with that name exists.
     */
    AbstractProject<?, ?> getJobByName(String name);
    
    /**
     * Returns all Hudson jobs.
     *
     * @return a list with all Hudson jobs. Never null.
     */
    @SuppressWarnings("unchecked")
    List<AbstractProject> getAllJobs();
    
    boolean isTopLevelJob(AbstractProject<?, ?> job);
    
    View getView(String viewName);
}
