package hudson.plugins.im;

import hudson.plugins.im.IMMessageTarget;
import hudson.plugins.im.tools.Assert;

/**
 * DefaultIMMessageTarget basically is a String, that represents an Im-Account to send messages to.
 * @author Pascal Bleser
 */
public class GroupChatIMMessageTarget implements IMMessageTarget
{
    private static final long serialVersionUID = 1L;
    protected String value;

    public GroupChatIMMessageTarget(final String value)
    {
        Assert.isNotNull(value, "Parameter 'value' must not be null.");
        this.value = value;
    }

    @Override
    public boolean equals(final Object arg0)
    {
        if (arg0 == null)
        {
            return false;
        }
        if (arg0 == this)
        {
            return true;
        }
        if (arg0 instanceof GroupChatIMMessageTarget)
        {
            final GroupChatIMMessageTarget other = (GroupChatIMMessageTarget) arg0;
            boolean retval = true;

            retval &= this.value.equals(other.value);

            return retval;
        }
        else
        {
            return false;
        }

    }

    @Override
    public int hashCode()
    {
        return this.value.hashCode();
    }

    @Override
    public String toString()
    {
        return this.value;
    }
}
