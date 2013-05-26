package hudson.plugins.im.bot;

import hudson.model.AbstractProject;
import hudson.model.View;

import java.util.List;

import jenkins.model.Jenkins;

/**
 * Default {@link JobProvider} which directly accesses {@link Jenkins#getInstance()}.
 *
 * @author kutzi
 */
public class DefaultJobProvider implements JobProvider {

    @Override
    public AbstractProject<?, ?> getJobByName(String name) {
        return Jenkins.getInstance().getItemByFullName(name, AbstractProject.class);
    }
    

    @SuppressWarnings("rawtypes")
    @Override
    public AbstractProject<?, ?> getJobByDisplayName(String displayName) {
        List<AbstractProject> allItems = Jenkins.getInstance().getAllItems(AbstractProject.class);
        for (AbstractProject job : allItems) {
            if (displayName.equals(job.getDisplayName())) {
                return job;
            }
        }
        return null;
    }

    @Override
    public AbstractProject<?, ?> getJobByNameOrDisplayName(String name) {
        AbstractProject<?,?> jobByName = getJobByName(name);
        return jobByName != null ? jobByName : getJobByDisplayName(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AbstractProject<?,?>> getAllJobs() {
        @SuppressWarnings("rawtypes")
        List items = Jenkins.getInstance().getAllItems(AbstractProject.class);
        return items;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<AbstractProject<?,?>> getTopLevelJobs() {
        @SuppressWarnings("rawtypes")
        List items = Jenkins.getInstance().getItems(AbstractProject.class);
        return items;
    }

    @Override
    public boolean isTopLevelJob(AbstractProject<?, ?> job) {
        return Jenkins.getInstance().equals(job.getParent());
    }

    @Override
    public View getView(String viewName) {
        return Jenkins.getInstance().getView(viewName);
    }
}
