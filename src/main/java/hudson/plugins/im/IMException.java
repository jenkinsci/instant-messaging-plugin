package hudson.plugins.im;

import java.io.IOException;

/**
 * Represents any kind of protocol-level error that may occur.
 * @author Uwe Schaefer
 */
// TODO: we should probably introduce more exception classes for more fine grained error reporting
public class IMException extends IOException
{
    private static final long serialVersionUID = 1L;

    public IMException(final Exception e) {
        super(e.getMessage());
        initCause(e);
    }
    
    public IMException(String msg) {
    	super(msg);
    }

}
