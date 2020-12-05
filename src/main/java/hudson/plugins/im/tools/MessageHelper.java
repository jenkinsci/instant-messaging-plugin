package hudson.plugins.im.tools;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import hudson.Util;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

/**
 * Utility class to help message creation
 *
 * @author vsellier
 * @author kutzi
 */
public class MessageHelper {
    private final static Pattern SPACE_PATTERN = Pattern.compile("\\s");
    private final static String QUOTE = "\"";

    /**
     * Returns the full URL to the build details page for a given build.
     */
    public static String getBuildURL(Run<?, ?> run) {
        if (run == null) {
            return "?";
        }

        // The hudson's base url
        final StringBuilder builder;
        if (Jenkins.getInstanceOrNull() != null) {
            builder = new StringBuilder(
                String.valueOf(Jenkins.get().getRootUrl()));
        } else {
            builder = new StringBuilder("null");
        }

        // The build's url, escaped for project with space or other specials
        // characters
        builder.append(Util.encode(run.getUrl()));

        return builder.toString();
    }

    /**
     * Returns the full URL to the test details page for a given test result;
     */
    public static String getTestUrl(hudson.tasks.test.TestResult result) {
        String buildUrl = getBuildURL(result.getOwner());
        @SuppressWarnings("rawtypes")
        AbstractTestResultAction action = result.getTestResultAction();

        TestObject parent = result.getParent();
        TestResult testResultRoot = null;
        while(parent != null) {
            if (parent instanceof TestResult) {
                testResultRoot = (TestResult) parent;
                break;
            }
            parent = parent.getParent();
        }

        String testUrl = action.getUrlName()
            + (testResultRoot != null ? testResultRoot.getUrl() : "")
            + result.getUrl();

        String[] pathComponents = testUrl.split("/");
        StringBuilder buf = new StringBuilder();
        for (String c : pathComponents) {
            buf.append(Util.rawEncode(c)).append('/');
        }
        // remove last /
        buf.deleteCharAt(buf.length() - 1);

        return buildUrl + buf.toString();
    }

    /**
     * Parses a bot command from a given string.
     * The 1st entry in the array contains the command name itself.
     * The following entries contain the command parameters, if any.
     */
    public static String[] extractCommandLine(String message) {
        List<String> parameters = extractParameters(message);
        return parameters.toArray(new String[parameters.size()]);
    }

    private static List<String> extractParameters(String commandLine) {
        List<String> parameters = new ArrayList<String>();

        // space protection
        commandLine = commandLine.trim();

        int firstQuote = commandLine.indexOf(QUOTE);

        if (firstQuote != -1) {
            int endQuoted = commandLine.indexOf(QUOTE, firstQuote + 1);

            if (endQuoted == -1) {
                //unmatched quotes, just split on spaces
                Collections.addAll(parameters, SPACE_PATTERN.split(commandLine));
            } else {
                if (firstQuote == 0) {
                    // the first character is the quote
                    parameters.add(commandLine.substring(1, endQuoted - 1));
                } else {
                    // adding every thing before the first quote
                    parameters.addAll(extractParameters(commandLine.substring(0,
                            firstQuote)));
                    // adding the parameter between quotes
                    parameters.add(commandLine.substring(firstQuote + 1, endQuoted));

                    // adding everything after the quoted parameter into the
                    // parameters list
                    if (endQuoted < commandLine.length() - 1) {
                        parameters.addAll(extractParameters(commandLine
                                .substring(endQuoted + 1)));
                    }
                }
            }
        } else {
            // no quotes, just splitting on spaces
            Collections.addAll(parameters, SPACE_PATTERN.split(commandLine));
        }
        return parameters;
    }

    /**
     * Copies the specified range of the specified array into a new array.
     * The initial index of the range (<tt>from</tt>) must lie between zero
     * and <tt>original.length</tt>, inclusive.  The value at
     * <tt>original[from]</tt> is placed into the initial element of the copy
     * (unless <tt>from == original.length</tt> or <tt>from == to</tt>).
     * Values from subsequent elements in the original array are placed into
     * subsequent elements in the copy.  The final index of the range
     * (<tt>to</tt>), which must be greater than or equal to <tt>from</tt>,
     * may be greater than <tt>original.length</tt>, in which case
     * <tt>(byte)0</tt> is placed in all elements of the copy whose index is
     * greater than or equal to <tt>original.length - from</tt>.  The length
     * of the returned array will be <tt>to - from</tt>.
     *
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to the final index of the range to be copied, exclusive.
     *     (This index may lie outside the array.)
     * @return a new array containing the specified range from the original array,
     *     truncated or padded with zeros to obtain the required length
     * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt>
     *     or <tt>from &gt; original.length()</tt>
     * @throws IllegalArgumentException if <tt>from &gt; to</tt>
     * @throws NullPointerException if <tt>original</tt> is null
     *
     * Note: Unfortunately in Java 5 there is no Arrays#copyOfRange, yet.
     * So we have to implement it ourself.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T[] copyOfRange(T[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        Class type = original.getClass();
        T[] copy = (type == Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(type.getComponentType(), newLength);
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copies the specified array, truncating or padding with nulls (if necessary)
     * so the copy has the specified length.  For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values.  For any indices that are valid in the
     * copy but not the original, the copy will contain <tt>null</tt>.
     * Such indices will exist if and only if the specified length
     * is greater than that of the original array.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with nulls
     *     to obtain the specified length
     * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
     * @throws NullPointerException if <tt>original</tt> is null
     *
     * Note: copied from java 6
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T[] copyOf(T[] original, int newLength) {
        Class type = original.getClass();
        T[] copy = (type == Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(type.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Returns a new array which a concatenation of the argument arrays.
     */
    public static <T> T[] concat(T[] array1, T[]... arrays) {
        int resultLength = array1.length;
        for (T[] array : arrays) {
            resultLength += array.length;
        }
        T[] result = copyOf(array1, resultLength);

        int offset = array1.length;
        for (T[] array : arrays) {
             for (int i=0; i < array.length; i++) {
                result[offset + i] = array[i];
             }
             offset += array.length;
        }
        return result;
    }

    /**
     * Joins together all strings in the array - starting at startIndex - by
     * using a single space as separator.
     */
    public static String join(String[] array, int startIndex) {
        String joined = StringUtils.join(copyOfRange(array, startIndex, array.length), " ");
        joined = joined.replaceAll("\"", "");
        return joined;
    }

    /**
     * Extracts a name from an argument array starting at a start index and removing
     * quoting ".
     *
     * @param args the arguments
     * @param startIndex the start index
     * @return the job name as a single String
     * @throws IndexOutOfBoundsException if startIndex > args length
     */
    public static String getJoinedName(String[] args, int startIndex) {
        String joined = join(args, startIndex);
        return joined.replaceAll("\"", "");
    }
}
