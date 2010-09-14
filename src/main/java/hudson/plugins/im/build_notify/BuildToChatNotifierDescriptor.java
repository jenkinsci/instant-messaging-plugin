package hudson.plugins.im.build_notify;

import hudson.model.Descriptor;

/**
 * {@link Descriptor} for {@link BuildToChatNotifier}.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.9
 */
public abstract class BuildToChatNotifierDescriptor extends Descriptor<BuildToChatNotifier> {
    /**
     * Return one line text that summarizes what this {@link BuildToChatNotifier} sends out.
     * This text is rendered into a &lt;select> control to allow the user to choose the
     * desired output verbosity.
     */
    public abstract String getDisplayName();
}
