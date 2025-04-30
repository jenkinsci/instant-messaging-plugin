package hudson.plugins.im.tools;

import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.ResultTrend;
import hudson.plugins.im.tools.BuildHelper.ExtResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuildHelperTest {

    @Test
    void testIsFix() {
        {
            FreeStyleBuild previousBuild = mock(FreeStyleBuild.class);
            when(previousBuild.getResult()).thenReturn(Result.FAILURE);

            FreeStyleBuild build = mock(FreeStyleBuild.class);
            when(build.getPreviousBuild()).thenReturn(previousBuild);

            // non non-successful build can ever be a fix:
            when(build.getResult()).thenReturn(Result.ABORTED);
            assertNotSame(ResultTrend.FIXED, ResultTrend.getResultTrend(build));

            when(build.getResult()).thenReturn(Result.NOT_BUILT);
            assertNotSame(ResultTrend.FIXED, ResultTrend.getResultTrend(build));

            when(build.getResult()).thenReturn(Result.UNSTABLE);
            assertNotSame(ResultTrend.FIXED, ResultTrend.getResultTrend(build));

            when(build.getResult()).thenReturn(Result.FAILURE);
            assertNotSame(ResultTrend.FIXED, ResultTrend.getResultTrend(build));

            // only a success can be a fix
            when(build.getResult()).thenReturn(Result.SUCCESS);
            assertSame(ResultTrend.FIXED, ResultTrend.getResultTrend(build));
        }

        {
            // a success without a previous failure cannot be a fix:
            FreeStyleBuild build = mock(FreeStyleBuild.class);
            when(build.getResult()).thenReturn(Result.SUCCESS);
            assertNotSame(ResultTrend.FIXED, ResultTrend.getResultTrend(build));
        }

        {
            // ABORTED doesn't count as failure
            FreeStyleBuild build = mock(FreeStyleBuild.class);
            when(build.getResult()).thenReturn(Result.ABORTED);
            assertNotSame(ResultTrend.FIXED, ResultTrend.getResultTrend(build));

            FreeStyleBuild nextBuild = mock(FreeStyleBuild.class);
            when(nextBuild.getResult()).thenReturn(Result.SUCCESS);
            when(nextBuild.getPreviousBuild()).thenReturn(build);

            assertNotSame(ResultTrend.FIXED, ResultTrend.getResultTrend(nextBuild));

            // but if there was a unstable/failing build somewhere before,
            // it is a fix again
            FreeStyleBuild anotherAborted = mock(FreeStyleBuild.class);
            when(build.getResult()).thenReturn(Result.ABORTED);

            FreeStyleBuild anUnstableBuild = mock(FreeStyleBuild.class);
            when(build.getResult()).thenReturn(Result.UNSTABLE);

            assertSame(ResultTrend.FIXED, ResultTrend.getResultTrend(nextBuild));
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    void testGetResultDescription() {
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        when(build.getResult()).thenReturn(Result.SUCCESS);
        assertEquals(ExtResult.SUCCESS.toString(), BuildHelper.getResultDescription(build));

        when(build.getResult()).thenReturn(Result.FAILURE);
        assertEquals(ExtResult.FAILURE.toString(), BuildHelper.getResultDescription(build));

        FreeStyleBuild previousBuild = mock(FreeStyleBuild.class);
        when(previousBuild.getResult()).thenReturn(Result.FAILURE);
        when(build.getPreviousBuild()).thenReturn(previousBuild);
        assertEquals(ExtResult.STILL_FAILING.toString(), BuildHelper.getResultDescription(build));

        when(build.getResult()).thenReturn(Result.SUCCESS);
        assertEquals(ExtResult.FIXED.toString(), BuildHelper.getResultDescription(build));

        when(build.getResult()).thenReturn(Result.UNSTABLE);
        assertEquals(ExtResult.NOW_UNSTABLE.toString(), BuildHelper.getResultDescription(build));

        when(previousBuild.getResult()).thenReturn(Result.UNSTABLE);
        assertEquals(ExtResult.STILL_UNSTABLE.toString(), BuildHelper.getResultDescription(build));

        when(build.getResult()).thenReturn(Result.SUCCESS);
        assertEquals(ExtResult.FIXED.toString(), BuildHelper.getResultDescription(build));

        when(previousBuild.getResult()).thenReturn(Result.ABORTED);
        assertEquals(ExtResult.ABORTED.toString(), BuildHelper.getResultDescription(previousBuild));

        when(build.getResult()).thenReturn(Result.SUCCESS);
        // don't mark it as fixed:
        assertEquals(ExtResult.SUCCESS.toString(), BuildHelper.getResultDescription(build));

        // NOW UNSTABLE
        when(previousBuild.getResult()).thenReturn(Result.FAILURE);
        when(build.getResult()).thenReturn(Result.UNSTABLE);
        assertEquals(ExtResult.NOW_UNSTABLE.toString(), BuildHelper.getResultDescription(build));
    }
}
