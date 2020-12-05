package hudson.plugins.im;

import hudson.Extension;
import hudson.Util;

import hudson.model.Descriptor;
import hudson.util.Secret;
import java.util.Objects;
import org.kohsuke.stapler.DataBoundConstructor;
import org.springframework.util.Assert;

/**
 * {@link GroupChatIMMessageTarget} represents a 'chat room' or something like that.
 */
public class GroupChatIMMessageTarget implements IMMessageTarget {
    private static final long serialVersionUID = 1L;

    /**
     * @deprecated replaced by name
     */
    @Deprecated
    private transient String value;

    private String name;
    private String password;
    private Secret secretPassword;
    private final boolean notificationOnly;

    /**
     * @deprecated use {@link GroupChatIMMessageTarget#GroupChatIMMessageTarget(String, String, boolean)}!
     */
    @Deprecated
    public GroupChatIMMessageTarget(final String name) {
        this(name, (Secret) null, false);
    }

    @Deprecated
    public GroupChatIMMessageTarget(String name, String password, boolean notificationOnly) {
        Assert.notNull(name, "Parameter 'name' must not be null.");
        this.name = name;
        this.secretPassword = Secret.fromString(password);
        this.notificationOnly = notificationOnly;
    }

    @DataBoundConstructor
    public GroupChatIMMessageTarget(String name, Secret secretPassword, boolean notificationOnly) {
        Assert.notNull(name, "Parameter 'name' must not be null.");
        this.name = name;
        this.secretPassword = secretPassword;
        this.notificationOnly = notificationOnly;
    }

    public String getName() {
        return name;
    }

    @Deprecated
    public String getPassword() {
        return password;
    }

    public Secret getSecretPassword() {
        return secretPassword;
    }

    public boolean hasPassword() {
        return this.secretPassword != null;
    }

    public boolean isNotificationOnly() {
        return this.notificationOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupChatIMMessageTarget that = (GroupChatIMMessageTarget) o;
        return notificationOnly == that.notificationOnly && 
            Objects.equals(value, that.value) && 
            Objects.equals(name, that.name) && 
            Objects.equals(secretPassword, that.secretPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, name, secretPassword, notificationOnly);
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Deserialize old instances.
     */
    protected Object readResolve() {
        if (this.value != null && this.name == null) {
            this.name = this.value;
        }
        
        if (this.password != null) {
            this.secretPassword = Secret.fromString(this.password);
            this.password = null;
        }
        
        this.value = null;
        return this;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<IMMessageTarget> {
        @Override
        public String getDisplayName() {
            return "groupChatIMMessageTarget";
        }
    }
}
