package hudson.plugins.im.bot;

import hudson.model.AbstractProject;
import hudson.model.View;

import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public interface JobProvider {
    /**
     * Returns the Jenkins job with the given name or null
     * if no job with that name exists.
     */
    @CheckForNull
    AbstractProject<?, ?> getJobByName(String name);
    
    /**
     * Returns all Jenkins jobs.
     *
     * @return a list with all Jenkins jobs. Never null.
     */
    @Nonnull
    List<AbstractProject<?,?>> getAllJobs();
    
    /**
     * Returns all top-level Jenkins jobs.
     *
     * @return a list with the top-level jobs. Never null.
     */
    @Nonnull
    List<AbstractProject<?,?>> getTopLevelJobs();
    
    boolean isTopLevelJob(AbstractProject<?, ?> job);
    
    /**
     * Return the view by name.
     * @param viewName the view name
     * @return the view or null, if no view by that name exists.
     */
    @CheckForNull
    View getView(String viewName);
}
