package hudson.plugins.im.bot;

import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.TopLevelItem;
import hudson.model.View;
import hudson.plugins.im.tools.MessageHelper;

import java.util.ArrayList;
import java.util.Collection;


/**
 * Abstract command which returns a result message for one or several jobs.
 *
 * @author kutzi
 */
abstract class AbstractMultipleJobCommand extends AbstractTextSendingCommand {
	
	static final String UNKNOWN_JOB_STR = "unknown job";
	static final String UNKNOWN_VIEW_STR = "unknown view";

	/**
	 * Returns the message to return for this job.
	 * Note that {@link AbstractMultipleJobCommand} already inserts one newline after each job's
	 * message so you don't have to do it yourself.
	 * 
	 * @param job The job
	 * @return the result message for this job
	 */
    protected abstract CharSequence getMessageForJob(AbstractProject<?, ?> job);

    /**
     * Returns a short name of the command needed for the help message
     * and as a leading descriptor in the result message.
     * 
     * @return short command name
     */
    protected abstract String getCommandShortName();
    
    private enum Mode {
    	SINGLE, VIEW, ALL;
    }

    @Override
	protected String getReply(String sender, String[] args) {
        Collection<AbstractProject<?, ?>> projects = new ArrayList<AbstractProject<?, ?>>();

        final Mode mode;
        String view = null;
        try {
            if (args.length >= 2) {
                if ("-v".equals(args[1])) {
                	mode = Mode.VIEW;
                	view = MessageHelper.getJoinedName(args, 2);
                    getProjectsForView(projects, view);
                } else {
                	mode = Mode.SINGLE;
                    String jobName = MessageHelper.getJoinedName(args, 1);

                    AbstractProject<?, ?> project = Hudson.getInstance().getItemByFullName(jobName, AbstractProject.class);
                    if (project != null) {
                        projects.add(project);
                    } else {
                    	return sender + ": " + UNKNOWN_JOB_STR + " " + jobName;
                    }
                }
            } else if (args.length == 1) {
            	mode = Mode.ALL;
                for (AbstractProject<?, ?> project : Hudson.getInstance().getAllItems(AbstractProject.class)) {
                    // add only top level project
                    // sub project are accessible by their name but are not shown for visibility
                    if (Hudson.getInstance().equals(project.getParent())) {
                        projects.add(project);
                    }
                }
            } else {
            	throw new IllegalArgumentException("'args' must not be empty!");
            }
        } catch (IllegalArgumentException e) {
            return sender + ": error: " + e.getMessage();
        }

        if (!projects.isEmpty()) {
            StringBuilder msg = new StringBuilder();
                
            switch(mode) {
            	case SINGLE : break;
            	case ALL:
            		msg.append(getCommandShortName())
            			.append(" of all projects:\n");
            		break;
            	case VIEW:
            		msg.append(getCommandShortName())
        				.append(" of projects in view " + view + ":\n");
            		break;
            }

            boolean first = true;
            for (AbstractProject<?, ?> project : projects) {
                if (!first) {
                    msg.append("\n");
                } else {
                    first = false;
                }

                msg.append(getMessageForJob(project));
            }
            return msg.toString();
        } else {
            return sender + ": no job found";
        }
	}

	public String getHelp() {
        return " [<job>|-v <view>] - show the "
                + getCommandShortName()
                + " of a specific job, jobs in a view or all jobs";
    }

    private void getProjectsForView(Collection<AbstractProject<?, ?>> toAddTo, String viewName) {
        View view = Hudson.getInstance().getView(viewName);

        if (view != null) {
            Collection<TopLevelItem> items = view.getItems();
            for (TopLevelItem item : items) {
                if (item instanceof AbstractProject<?, ?>) {
                    toAddTo.add((AbstractProject<?, ?>) item);
                }
            }
        } else {
            throw new IllegalArgumentException(UNKNOWN_VIEW_STR + ": " + viewName);
        }
    }
}
