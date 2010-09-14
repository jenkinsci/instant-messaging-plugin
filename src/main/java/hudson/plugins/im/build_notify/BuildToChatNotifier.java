package hudson.plugins.im.build_notify;

import hudson.DescriptorExtensionList;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.plugins.im.IMPublisher;

import java.io.IOException;

/**
 * Controls the exact messages to be sent to a chat upon start/completion of a build.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.9
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
     * @param build
     *      The build that just completed. Never null.
     * @param listener
     *      Any errors can be reported here.
     */
    public abstract String buildCompletionMessage(IMPublisher publisher, AbstractBuild<?, ?> build, BuildListener listener)
            throws IOException,  InterruptedException;


    @Override
    public BuildToChatNotifierDescriptor getDescriptor() {
        return (BuildToChatNotifierDescriptor) Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * All the registered {@link BuildToChatNotifier} types.
     */
    public static DescriptorExtensionList<BuildToChatNotifier,BuildToChatNotifierDescriptor> all() {
        return Hudson.getInstance().getDescriptorList(BuildToChatNotifier.class);
    }
}
