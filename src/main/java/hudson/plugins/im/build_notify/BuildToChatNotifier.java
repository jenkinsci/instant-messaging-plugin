package hudson.plugins.im.build_notify;

import hudson.DescriptorExtensionList;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.im.IMPublisher;
import hudson.plugins.im.tools.MessageHelper;

import java.io.IOException;

import static hudson.plugins.im.tools.BuildHelper.*;

/**
 * Controls the exact messages to be sent to a chat upon start/completion of a build.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.10
 */
public abstract class BuildToChatNotifier implements Describable<BuildToChatNotifier> {
    /**
     * Calculates the message to send out to a chat when the specified build is started.
     *
     * @param publisher
     *      The publisher that's driving this. Never null.
     * @param build
     *      The build that just completed. Never null.
     * @param listener
     *      Any errors can be reported here.
     */
    public abstract String buildStartMessage(IMPublisher publisher, AbstractBuild<?, ?> build, BuildListener listener)
            throws IOException,  InterruptedException;


    /**
     * Calculates the message to send out to a chat when the specified build is completed.
     *
     * @param publisher
     *      The publisher that's driving this. Never null.
     * @param run
     *      The build that just completed. Never null.
     * @param listener
     */
    public abstract String buildCompletionMessage(IMPublisher publisher, Run<?, ?> run, TaskListener listener)
            throws IOException,  InterruptedException;

    /**
     * Calculates the message to send out to a committer of a broken build.
     *
     * @param publisher
     *      The publisher that's driving this. Never null.
     * @param run
     *      The build that just completed. Never null.
     * @param listener
     *      Any errors can be reported here.
     * @param firstFailure
     */
    public String suspectMessage(IMPublisher publisher, Run<?, ?> run, TaskListener listener, boolean firstFailure) {
        if (firstFailure) {
            return "Oh no! You're suspected of having broken " + getProjectName(run) + ": " + MessageHelper.getBuildURL(run);
        } else {
            return "Build " + getProjectName(run) +
                " is " + getResultTrend(run).getID() + ": " + MessageHelper.getBuildURL(run);
        }
    }

    /**
     * Calculates the message to send out to a 'culprit' of a broken build.
     * I.e. a committer to a previous build which was broken and all builds since then
     * have been broken.
     *
     * @param publisher
     *      The publisher that's driving this. Never null.
     * @param run
     *      The build that just completed. Never null.
     * @param listener
     */
    public String culpritMessage(IMPublisher publisher, Run<?, ?> run, TaskListener listener) {
        return "You're still being suspected of having broken " + getProjectName(run) + ": " + MessageHelper.getBuildURL(run);
    }

    /**
     * Calculates the message to send out to a committer of a fixed build.
     *
     * @param publisher
     *      The publisher that's driving this. Never null.
     * @param run
     *      The build that just completed. Never null.
     * @param listener
     *      Any errors can be reported here.
     */
    public String fixerMessage(IMPublisher publisher, Run<?, ?> run, TaskListener listener) {
        return "Yippee! Seems you've fixed " + getProjectName(run) + ": " + MessageHelper.getBuildURL(run);
    }

    /**
     * Calculates the message to send out to a committer of an upstream build
     * if this build is broken.
     *
     * @param publisher
     *      The publisher that's driving this. Never null.
     * @param run
     *      The build that just completed. Never null.
     * @param listener
     *      Any errors can be reported here.
     * @param upstreamBuild
     */
    public String upstreamCommitterMessage(IMPublisher publisher, Run<?, ?> run, TaskListener listener,
            Run<?, ?> upstreamBuild) {
        return "Attention! Your change in " + getProjectName(upstreamBuild)
            + ": " + MessageHelper.getBuildURL(upstreamBuild)
            + " *might* have broken the downstream job " + getProjectName(run) + ": " + MessageHelper.getBuildURL(run)
            + "\nPlease have a look!";
    }


    @Override
    public BuildToChatNotifierDescriptor getDescriptor() {
        return (BuildToChatNotifierDescriptor) Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * All the registered {@link BuildToChatNotifier} types.
     */
    public static DescriptorExtensionList<BuildToChatNotifier,BuildToChatNotifierDescriptor> all() {
        return Hudson.getInstance().<BuildToChatNotifier,BuildToChatNotifierDescriptor>getDescriptorList(BuildToChatNotifier.class);
    }
}
