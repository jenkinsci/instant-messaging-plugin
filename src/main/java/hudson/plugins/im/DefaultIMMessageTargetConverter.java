package hudson.plugins.im;

import hudson.plugins.im.tools.Assert;
import hudson.plugins.im.DefaultIMMessageTarget;

/**
 * Default implementation of {@link IMMessageTargetConverter}.
 * 
 * Just uses the toString methods of the target resp. returns a
 * {@link DefaultIMMessageTarget}.
 */
public class DefaultIMMessageTargetConverter implements IMMessageTargetConverter
{

    /**
     * {@inheritDoc}
     */
    public IMMessageTarget fromString(final String targetAsString) throws IMMessageTargetConversionException
    {
        if (targetAsString != null)
        {
            final String f = targetAsString.trim();
            if (f.length() > 0)
            {
                return new DefaultIMMessageTarget(f);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String toString(final IMMessageTarget target)
    {
        Assert.isNotNull(target, "Parameter 'target' must not be null.");
        return target.toString();
    }
}