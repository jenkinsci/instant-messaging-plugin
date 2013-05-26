package hudson.plugins.im.bot;

import hudson.model.AbstractProject;
import hudson.model.TopLevelItem;
import hudson.model.View;
import hudson.plugins.im.Sender;
import hudson.plugins.im.tools.MessageHelper;
import hudson.plugins.im.tools.Pair;

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
    
    enum Mode {
    	SINGLE, VIEW, ALL;
    }

    @Override
	protected String getReply(Bot bot, Sender sender, String[] args) {
    	
//    	if (!authorizationCheck()) {
//    		return "Sorry, can't do that!";
//    	}

        Collection<AbstractProject<?, ?>> projects = new ArrayList<AbstractProject<?, ?>>();

        final Pair<Mode, String> pair;
        try {
            pair = getProjects(sender, args, projects);
        } catch (CommandException e) {
            return getErrorReply(sender, e);
        }

        if (!projects.isEmpty()) {
            StringBuilder msg = new StringBuilder();
                
            switch(pair.getHead()) {
            	case SINGLE : break;
            	case ALL:
            		msg.append(getCommandShortName())
            			.append(" of all projects:\n");
            		break;
            	case VIEW:
            		msg.append(getCommandShortName())
        				.append(" of projects in view " + pair.getTail() + ":\n");
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
    
    /**
     * Returns a list of projects for the given arguments.
     * 
     * @param projects the list to which the projects are added
     * @return a pair of Mode (single job, jobs from view or all) and view name -
     * where view name will be null if mode != VIEW
     */
    Pair<Mode, String> getProjects(Sender sender, String[] args, Collection<AbstractProject<?, ?>> projects)
        throws CommandException {
        final Mode mode;
        String view = null;
        if (args.length >= 2) {
            if ("-v".equals(args[1])) {
                mode = Mode.VIEW;
                view = MessageHelper.getJoinedName(args, 2);
                getProjectsForView(projects, view);
            } else {
                mode = Mode.SINGLE;
                String jobName = MessageHelper.getJoinedName(args, 1);

                AbstractProject<?, ?> project = getJobProvider().getJobByNameOrDisplayName(jobName);
                if (project != null) {
                    projects.add(project);
                } else {
                    throw new CommandException(sender.getNickname() + ": " + UNKNOWN_JOB_STR + " " + jobName);
                }
            }
        } else if (args.length == 1) {
            mode = Mode.ALL;
            // don't show really all - could by quite many - but only the top-level jobs
            projects.addAll(getJobProvider().getTopLevelJobs());
        } else {
            throw new CommandException(sender + ": 'args' must not be empty!");
        }
        return Pair.create(mode, view);
    }

	public String getHelp() {
        return " [<job>|-v <view>] - show the "
                + getCommandShortName()
                + " of a specific job, jobs in a view or all jobs";
    }

    private void getProjectsForView(Collection<AbstractProject<?, ?>> toAddTo, String viewName) {
        View view = getJobProvider().getView(viewName);

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
