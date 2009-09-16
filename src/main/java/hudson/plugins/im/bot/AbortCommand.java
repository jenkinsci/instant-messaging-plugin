package hudson.plugins.im.bot;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Executor;
import hudson.model.Hudson;

/**
 * Abort a running job
 * @author R. Tyler Ballance <tyler@slide.com>
 */
public class AbortCommand extends AbstractSingleJobCommand {
	
	private static final String HELP = " <job> - specify which job to abort";

	public String getHelp() {
		return HELP;
	}

    @Override
    protected CharSequence getMessageForJob(AbstractProject<?, ?> project, String sender) {
        if ( (project.isInQueue() == false) && (project.isBuilding() == false) ) {
            return sender + ": how do you intend a build that isn't building?";
        }
        
        boolean aborted = false;
        if (project.isInQueue()) {
            aborted = Hudson.getInstance().getQueue().cancel(project);
        }
        
        if (!aborted) {
            // must be already building
            AbstractBuild<?, ?> build = project.getLastBuild();
            if (build == null) {
                // No builds? lolwut?
                return sender + ": it appears this job has never been built";
            }   

            Executor ex = build.getExecutor();
            if (ex == null) {
                aborted = false; // how the hell does this happen o_O
            } else {
                ex.interrupt();
            }
        }

        if (aborted) {
            return project.getName() + " aborted, I hope you're happy!";
        } else {
            return sender + ": " + " couldn't abort " + project.getName() + ". I don't know why this happened.";
        }
    }

}
