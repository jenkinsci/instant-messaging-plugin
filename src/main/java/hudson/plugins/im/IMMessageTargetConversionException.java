package hudson.plugins.im;

/**
 * Signals a conversion Exception from String to IMMessageTarget
 * @author Uwe Schaefer
 */
public class IMMessageTargetConversionException extends Exception
{
    private static final long serialVersionUID = 1L;

    public IMMessageTargetConversionException(final String message)
    {
        super(message);
    }
}
