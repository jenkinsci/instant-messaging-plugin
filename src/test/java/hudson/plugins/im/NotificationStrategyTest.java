package hudson.plugins.im;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import org.junit.jupiter.api.Test;

import static hudson.model.Result.ABORTED;
import static hudson.model.Result.FAILURE;
import static hudson.model.Result.NOT_BUILT;
import static hudson.model.Result.SUCCESS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationStrategyTest {

    @Test
    void testAll() {
        NotificationStrategy strategy = NotificationStrategy.ALL;
        testNewFailure(strategy, true);
        testRepeatFailure(strategy, true);
        testFixed(strategy, true);
        testRepeatSuccess(strategy, true);
        testNewAborted(strategy, true);
        testRepeatAborted(strategy, true);
        testNewNotBuilt(strategy, true);
        testRepeatNotBuilt(strategy, true);
    }

    @Test
    void testAnyFailure() {
        NotificationStrategy strategy = NotificationStrategy.ANY_FAILURE;
        testNewFailure2(strategy, true);
        testRepeatFailure(strategy, true);
        testFixed(strategy, false);
        testRepeatSuccess(strategy, false);

        // ANY_FAILURE notifies on any unsuccessful build, including aborted and not built.
        // UNSUCCESSFUL would be a better name.
        testNewAborted(strategy, true);
        testRepeatAborted(strategy, true);
        testNewNotBuilt(strategy, true);
        testRepeatNotBuilt(strategy, true);
    }

    @Test
    void testFailureAndFixed() {
        NotificationStrategy strategy = NotificationStrategy.FAILURE_AND_FIXED;
        testNewFailure2(strategy, true);
        testRepeatFailure(strategy, true);
        testFixed(strategy, true);
        testRepeatSuccess(strategy, false);

        // FAILURE_AND_FIXED notifies on any unsuccessful build, including aborted and not built.
        // UNSUCCESSFUL_AND_FIXED would be a better name.
        testNewAborted(strategy, true);
        testRepeatAborted(strategy, true);
        testNewNotBuilt(strategy, true);
        testRepeatNotBuilt(strategy, true);
    }

    @Test
    void testNewFailureAndFixed() {
        NotificationStrategy strategy = NotificationStrategy.NEW_FAILURE_AND_FIXED;
        testNewFailure(strategy, true);
        testRepeatFailure(strategy, false);
        testFixed(strategy, true);
        testRepeatSuccess(strategy, false);
        testNewAborted(strategy, false);
        testRepeatAborted(strategy, false);
        testNewNotBuilt(strategy, false);
        testRepeatNotBuilt(strategy, false);
    }

    @Test
    void testStateChangeOnly() {
        NotificationStrategy strategy = NotificationStrategy.STATECHANGE_ONLY;
        testNewFailure3(strategy, true);
        testRepeatFailureStrict(strategy, false);
        testFixed(strategy, true);
        testRepeatSuccessStrict(strategy, false);
        testNewAborted(strategy, true);
        testRepeatAborted(strategy, false);
        testNewNotBuilt(strategy, true);
        testRepeatNotBuilt(strategy, false);
    }

    private void testNewFailure(NotificationStrategy strategy, boolean expected) {
        // Basic success -> new failure
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, FAILURE)), equalTo(expected));

        // Failure on first build.
        assertThat(strategy.notificationWanted(historyOf(FAILURE)), equalTo(expected));

        // Intermediate ABORTED and NOT_BUILT states do not affect result
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, ABORTED, FAILURE)),
            equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, NOT_BUILT, FAILURE)),
            equalTo(expected));
    }

    private void testRepeatFailure(NotificationStrategy strategy, boolean expected) {
        testRepeatFailureStrict(strategy, expected);

        // Intermediate ABORTED and NOT_BUILT states do not affect result
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, ABORTED, FAILURE, ABORTED, FAILURE)),
            equalTo(expected));
        assertThat(
            strategy.notificationWanted(historyOf(SUCCESS, NOT_BUILT, FAILURE, NOT_BUILT, FAILURE)),
            equalTo(expected));
        assertThat(
            strategy.notificationWanted(historyOf(SUCCESS, NOT_BUILT, FAILURE, ABORTED, FAILURE)),
            equalTo(expected));
    }

    private void testRepeatFailureStrict(NotificationStrategy strategy, boolean expected) {
        // Basic success -> new failure -> repeat failure
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, FAILURE, FAILURE)),
            equalTo(expected));

        // Repeat failure on second build.
        assertThat(strategy.notificationWanted(historyOf(FAILURE, FAILURE)), equalTo(expected));
    }

    private void testFixed(NotificationStrategy strategy, boolean expected) {
        // Basic failure -> fixed
        assertThat(strategy.notificationWanted(historyOf(FAILURE, SUCCESS)), equalTo(expected));

        // Intermediate ABORTED and NOT_BUILT states do not affect result
        assertThat(strategy.notificationWanted(historyOf(FAILURE, ABORTED, SUCCESS)),
            equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(FAILURE, NOT_BUILT, SUCCESS)),
            equalTo(expected));
    }

    private void testRepeatSuccess(NotificationStrategy strategy, boolean expected) {
        testRepeatSuccessStrict(strategy, expected);

        // Intermediate ABORTED and NOT_BUILT states do not affect result
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, ABORTED, SUCCESS)),
            equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, NOT_BUILT, SUCCESS)),
            equalTo(expected));
    }

    private void testRepeatSuccessStrict(NotificationStrategy strategy, boolean expected) {
        // Basic failure -> fixed -> repeat success
        assertThat(strategy.notificationWanted(historyOf(FAILURE, SUCCESS, SUCCESS)),
            equalTo(expected));
    }

    private void testNewAborted(NotificationStrategy strategy, boolean expected) {
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, ABORTED)), equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(FAILURE, ABORTED)), equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(NOT_BUILT, ABORTED)), equalTo(expected));
    }

    private void testRepeatAborted(NotificationStrategy strategy, boolean expected) {
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, ABORTED, ABORTED)),
            equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(FAILURE, ABORTED, ABORTED)),
            equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(NOT_BUILT, ABORTED, ABORTED)),
            equalTo(expected));
    }

    private void testNewNotBuilt(NotificationStrategy strategy, boolean expected) {
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, NOT_BUILT)), equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(FAILURE, NOT_BUILT)), equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(ABORTED, NOT_BUILT)), equalTo(expected));
    }

    private void testRepeatNotBuilt(NotificationStrategy strategy, boolean expected) {
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, NOT_BUILT, NOT_BUILT)),
            equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(FAILURE, NOT_BUILT, NOT_BUILT)),
            equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(ABORTED, NOT_BUILT, NOT_BUILT)),
            equalTo(expected));
    }

    /** Construct a history of builds with the specified results (oldest result first). */
    private AbstractBuild historyOf(Result... results) {
        AbstractBuild toRet = null;
        for (Result result : results) {
            AbstractBuild build = mock(AbstractBuild.class);
            when(build.getResult()).thenReturn(result);
            when(build.getPreviousBuild()).thenReturn(toRet);
            toRet = build;
        }
        return toRet;
    }

    private AbstractBuild historyOf2(Result... results) {
        AbstractBuild toRet = null;
        for (Result result : results) {
            AbstractBuild build = mock(AbstractBuild.class);
            when(build.getResult()).thenReturn(result);
            toRet = build;
        }
        return toRet;
    }

    private void testNewFailure2(NotificationStrategy strategy, boolean expected) {
        // Basic success -> new failure
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, FAILURE)), equalTo(expected));
        // Failure on first build.
        assertThat(strategy.notificationWanted(historyOf2(FAILURE)), equalTo(expected));
        // Intermediate ABORTED and NOT_BUILT states do not affect result
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, ABORTED, FAILURE)), equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, NOT_BUILT, FAILURE)), equalTo(expected));
    }

    private AbstractBuild historyOf3(Result... results) {
        AbstractBuild toRet = null;
        for (int i = 0; i < results.length; i++) {
            AbstractBuild build = mock(AbstractBuild.class);
            when(build.getPreviousBuild()).thenReturn(toRet);
            toRet = build;
        }
        return toRet;
    }

    private void testNewFailure3(NotificationStrategy strategy, boolean expected) {
        // Basic success -> new failure
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, FAILURE)), equalTo(expected));
        // Failure on first build.
        assertThat(strategy.notificationWanted(historyOf3(FAILURE)), equalTo(expected));
        // Intermediate ABORTED and NOT_BUILT states do not affect result
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, ABORTED, FAILURE)), equalTo(expected));
        assertThat(strategy.notificationWanted(historyOf(SUCCESS, NOT_BUILT, FAILURE)), equalTo(expected));
    }

}
