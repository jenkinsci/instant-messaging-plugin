package hudson.plugins.im.bot;

import hudson.model.AbstractProject;
import hudson.plugins.im.Sender;
import hudson.plugins.im.tools.MessageHelper;
import hudson.security.Permission;

/**
 * Abstract command which works on a single job.
 *
 * @author kutzi
 */
abstract class AbstractSingleJobCommand extends AbstractTextSendingCommand {

    private final int numberOfArguments;
    
    protected AbstractSingleJobCommand() {
        this(0);
    }
    
    /**
     * @param numberOfArguments The number of arguments (in addition to the job name)
     * required by this command. Number of actual specified arguments may be equal or greater.
     */
    protected AbstractSingleJobCommand(int numberOfArguments) {
        this.numberOfArguments = numberOfArguments;
    }

    /**
     * Returns the message to return for this job.
     * 
     * Implementors should only return a String if the command
     * was executed successfully. Otherwise a {@link CommandException}
     * should be thrown!
     * 
     * @param job The job
     * @param sender The sender of the command
     * @return the result message for this job if the command was executed successfully
     * @throws CommandException if the command couldn't be executed for any reason
     */
    protected abstract CharSequence getMessageForJob(AbstractProject<?, ?> job, Sender sender,
            String[] arguments) throws CommandException;
    
    protected abstract Permission getRequiredPermission();

    @Override
    protected String getReply(Bot bot, Sender sender, String[] args) {
        if (args.length > 1 + numberOfArguments) {
            final String jobName;
            final String[] remainingArgs;
            if (this.numberOfArguments == 0) {
                jobName  = MessageHelper.getJoinedName(args, 1);
                remainingArgs = new String[0];
            } else {
                jobName = args[1].replace("\"", "");
                remainingArgs = MessageHelper.copyOfRange(args, 2, args.length);
            }
            AbstractProject<?, ?> job = getJobProvider().getJobByNameOrDisplayName(jobName);
            if (job != null) {
                if (!job.hasPermission(getRequiredPermission())) {
                    return "You don't have the permissions to perform this command on this job.";
                }
                
                try {
                    return getMessageForJob(job, sender, remainingArgs).toString();
                } catch (CommandException e) {
                    return getErrorReply(sender, e);
                }
            } else {
                return sender + ": unknown job '" + jobName + "'";
            }
        } else {
            if (this.numberOfArguments == 0) {
                return sender + ": you must specify a job name";
            } else {
                return sender + ": you must specify a job name and " + this.numberOfArguments +
                 " additional arguments";
            }
        }
    }
}
