package hudson.plugins.im;

import hudson.Extension;

import hudson.model.Descriptor;
import hudson.util.Secret;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static java.util.Objects.requireNonNull;

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
    private Secret password;
    @Deprecated
    private transient Secret secretPassword;
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
        requireNonNull(name, "Parameter 'name' must not be null.");
        this.name = name;
        this.password = Secret.fromString(password);
        this.notificationOnly = notificationOnly;
    }

    public GroupChatIMMessageTarget(String name, Secret secretPassword, boolean notificationOnly) {
        requireNonNull(name, "Parameter 'name' must not be null.");
        this.name = name;
        this.password = secretPassword;
        this.notificationOnly = notificationOnly;
    }

    @DataBoundConstructor
    public GroupChatIMMessageTarget(String name, boolean notificationOnly) {
        requireNonNull(name, "Parameter 'name' must not be null.");
        this.name = name;
        this.notificationOnly = notificationOnly;
    }

    public String getName() {
        return name;
    }

    @Deprecated
    public String getPassword() {
        return Secret.toString(password);
    }

    public Secret getSecretPassword() {
        return password;
    }

    @DataBoundSetter
    public void setSecretPassword(Secret secretPassword) {
        if (Secret.toString(secretPassword).equals("")) {
            this.password = null;
        } else {
            this.password = secretPassword;
        }
    }

    public boolean hasPassword() {
        return this.password != null && StringUtils.isNotBlank(this.password.getPlainText());
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
            Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, name, password, notificationOnly);
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
        
        if (this.secretPassword != null) {
            this.password = this.secretPassword;
            this.secretPassword = null;
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
