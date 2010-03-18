package hudson.plugins.im;

import hudson.Util;
import hudson.plugins.im.IMMessageTarget;
import hudson.plugins.im.tools.Assert;

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

    public GroupChatIMMessageTarget(final String name) {
        this(name, null);
    }
    
    public GroupChatIMMessageTarget(String name, String password) {
    	Assert.isNotNull(name, "Parameter 'name' must not be null.");
    	this.name = name;
    	this.password = password;
    }
    
    public String getName() {
		return name;
	}

	public String getPassword() {
		return password;
	}
	
	public boolean hasPassword() {
		return Util.fixEmpty(this.password) != null;
	}

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((password == null) ? 0 : password.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GroupChatIMMessageTarget other = (GroupChatIMMessageTarget) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		return true;
	}

	@Override
    public String toString() {
        return this.name;
    }
	
	/**
	 * Deserialize old instances.
	 */
	private Object readResolve() {
		if (this.value != null && this.name == null) {
			this.name = this.value;
		}
		this.value = null;
		return this;
	}
}
