package hudson.plugins.im;

/**
 * A IMMessageTargetConverter has the responsibility of creating a 
 * IMMessageTarget from a String and back. It will be used to create 
 * IMMessageTargets from user input and to display them back through 
 * the GUI. Note that fromString and toString should be at least 
 * semantically symmetric.
 *    
 * @author Uwe Schaefer
 *
 */
public interface IMMessageTargetConverter
{
    /**
     * Creates an {@link IMMessageTarget} from the given String.
     *
     * @param targetAsString can be null 
     * @return might return null, if input was null or an empty String 
     */
    IMMessageTarget fromString(String targetAsString) throws IMMessageTargetConversionException;

    /**
     * Turns given {@link IMMessageTarget} into a String for GUI-Display.
     *
     * @param target must not be null
     * @return String representation of the target
     */
    String toString(IMMessageTarget target);
}
