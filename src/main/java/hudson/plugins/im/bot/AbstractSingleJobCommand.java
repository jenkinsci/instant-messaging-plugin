package hudson.plugins.im.bot;

import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.plugins.im.tools.MessageHelper;

/**
 * Abstract job which works on a single job.
 *
 * @author kutzi
 */
abstract class AbstractSingleJobCommand extends AbstractTextSendingCommand {

    /**
     * Returns the message to return for this job.
     * 
     * @param job The job
     * @param sender The sender of the command
     * @return the result message for this job
     */
    protected abstract CharSequence getMessageForJob(AbstractProject<?, ?> job, String sender);

    @Override
    protected String getReply(String sender, String[] args) {
        if (args.length > 1) {
            String jobName = MessageHelper.getJoinedName(args, 1);
            AbstractProject<?, ?> job = Hudson.getInstance().getItemByFullName(jobName, AbstractProject.class);
            if (job != null) {
                return getMessageForJob(job, sender).toString();
            } else {
                return sender + ": unknown job " + jobName;
            }
        } else {
            return sender + ": you must specify a job name";
        }
    }
}
