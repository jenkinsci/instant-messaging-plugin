package hudson.plugins.im.bot;

import hudson.model.AbstractProject;
import hudson.model.Hudson;

public class DefaultJobProvider implements JobProvider {

    @Override
    public AbstractProject<?, ?> getJobByName(String name) {
        return Hudson.getInstance().getItemByFullName(name, AbstractProject.class);
    }

}
