package hudson.plugins.im.tools;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Convenience
 * @author Uwe Schaefer
 *
 */
public class ExceptionHelper
{
    /**
     * @param throwable
     * @return StackTrace of given throwable as a String
     */
    public static final String dump(final Throwable throwable)
    {
        if (throwable == null)
        {
            return "null";
        }
        final StringWriter sw = new StringWriter(512);
        throwable.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }
}
