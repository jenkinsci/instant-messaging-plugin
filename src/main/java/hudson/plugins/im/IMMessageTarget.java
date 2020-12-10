package hudson.plugins.im;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import java.io.Serializable;
import jenkins.model.Jenkins;

/**
 * A MessageTarget represents a user to send notifications to (like "peter@jabber.org").
 *
 * Usually, you don't need to implement this interface.
 * Instead, please use {@link DefaultIMMessageTarget} and {@link GroupChatIMMessageTarget}.
 *
 * @author Uwe Schaefer
 */
public interface IMMessageTarget extends Serializable, Describable<IMMessageTarget> {

    @SuppressWarnings("unchecked")
    default Descriptor<IMMessageTarget> getDescriptor() {
        return (Descriptor<IMMessageTarget>) Jenkins.get().getDescriptorOrDie(getClass());
    }

    @Extension
    class DescriptorImpl extends Descriptor<IMMessageTarget> {
        @Override
        public String getDisplayName() {
            return "IM Message Target";
        }
    }
}
