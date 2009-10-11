package hudson.plugins.im.bot;

import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.View;

import java.util.List;

/**
 * Default {@link JobProvider} which directly accesses {@link Hudson#getInstance()}.
 *
 * @author kutzi
 */
public class DefaultJobProvider implements JobProvider {

    @Override
    public AbstractProject<?, ?> getJobByName(String name) {
        return Hudson.getInstance().getItemByFullName(name, AbstractProject.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AbstractProject> getAllJobs() {
        return Hudson.getInstance().getAllItems(AbstractProject.class);
    }

    @Override
    public boolean isTopLevelJob(AbstractProject<?, ?> job) {
        return Hudson.getInstance().equals(job.getParent());
    }

    @Override
    public View getView(String viewName) {
        return Hudson.getInstance().getView(viewName);
    }
}
