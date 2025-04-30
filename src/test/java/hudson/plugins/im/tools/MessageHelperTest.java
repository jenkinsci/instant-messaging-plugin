package hudson.plugins.im.tools;

import hudson.model.AbstractBuild;
import hudson.plugins.im.tools.MessageHelper;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageHelperTest {

    @Test
    void testExtractCommandLine() {
        assertEquals(1, MessageHelper.extractCommandLine("test").length);
        assertEquals(1, MessageHelper.extractCommandLine("test  ").length);
        assertEquals(3,
                MessageHelper.extractCommandLine("test param1 param2").length);

        assertEquals(3, MessageHelper.extractCommandLine("param1 \"same param\" param3").length);
        assertEquals("same param", MessageHelper.extractCommandLine("param1 \"same param\" param3")[1]);
        assertEquals(2,
                MessageHelper.extractCommandLine("test \"same param\"").length);
        // ' is not a separator
        assertEquals(2, MessageHelper
                .extractCommandLine("param1 \"test 'same param'\"").length);

        // several quoted arguments
        assertEquals(3, MessageHelper
                .extractCommandLine("param1 \"second param\" \"third param\"").length);

        assertEquals(3, MessageHelper
                .extractCommandLine("param1 param's param3").length);

        assertEquals(1, MessageHelper.extractCommandLine("\"param1 param2\"").length);
    }

    @Test
    @Issue("JENKINS-3215")
    void testSingleQuote() {
        String cmdLine = "\"";
        assertEquals(1, MessageHelper.extractCommandLine(cmdLine).length);
        assertEquals("\"", MessageHelper.extractCommandLine(cmdLine)[0]);

        cmdLine = "\"a b";
        assertEquals(2, MessageHelper.extractCommandLine(cmdLine).length);
        assertEquals("\"a", MessageHelper.extractCommandLine(cmdLine)[0]);

        cmdLine = "a b\"";
        assertEquals(2, MessageHelper.extractCommandLine(cmdLine).length);
        assertEquals("b\"", MessageHelper.extractCommandLine(cmdLine)[1]);
    }

    @Test
    void testConcat() {
        String[] a = {"a"};
        String[] b = {"b"};
        String[] c = {"c"};

        String[] concat = MessageHelper.concat(a, b, c);
        assertArrayEquals(new String[] {"a", "b", "c"}, concat);

        concat = MessageHelper.concat(a);
        assertArrayEquals(a, concat);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void testUrlShouldBeUrlEncoded() {
        TestResult result = mock(TestResult.class);
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getUrl()).thenReturn("/a build");

        AbstractTestResultAction action = mock(AbstractTestResultAction.class);
        when(action.getUrlName()).thenReturn("/action");

        when(result.getOwner()).thenReturn(build);
        when(result.getTestResultAction()).thenReturn(action);
        when(result.getUrl()).thenReturn("/some id with spaces");

        String testUrl = MessageHelper.getTestUrl(result);
        assertEquals("null/a%20build/action/some%20id%20with%20spaces", testUrl);
    }
}
