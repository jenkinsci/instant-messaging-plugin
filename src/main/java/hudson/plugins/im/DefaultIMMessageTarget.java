package hudson.plugins.im;

import org.springframework.util.Assert;

/**
 * {@link DefaultIMMessageTarget} basically is a String, that represents an IM-Account to send messages to.
 * @author Uwe Schaefer
 */
public class DefaultIMMessageTarget implements IMMessageTarget
{
    private static final long serialVersionUID = 1L;
    protected final String value;

    public DefaultIMMessageTarget(final String value)
    {
        Assert.notNull(value, "Parameter 'value' must not be null.");
        this.value = value;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (o == null)
        {
            return false;
        }
        if (o == this)
        {
            return true;
        }
        if (o instanceof DefaultIMMessageTarget)
        {
            final DefaultIMMessageTarget other = (DefaultIMMessageTarget) o;
            return this.value.equals(other.value);
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