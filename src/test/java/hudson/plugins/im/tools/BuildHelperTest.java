package hudson.plugins.im.tools;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

import java.util.concurrent.TimeUnit;

import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.HudsonTestCase;

public class BuildHelperTest extends HudsonTestCase {
    
    public void testIsFix() throws Exception {
        {
            FreeStyleProject project = createFreeStyleProject();
            project.getBuildersList().add(new FailureBuilder());
            AbstractBuild<?, ?> build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
            assertFalse(BuildHelper.isFix(build));
            
            project.getBuildersList().remove(FailureBuilder.class);
            build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
            assertTrue(BuildHelper.isFix(build));
        }
        
        {
            FreeStyleProject project = createFreeStyleProject();
            AbstractBuild<?, ?> build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
            assertFalse(BuildHelper.isFix(build));
        }
        
        // test with aborted build
        {
            FreeStyleProject project = createFreeStyleProject();
            project.getBuildersList().add(new MockBuilder(Result.ABORTED));
            AbstractBuild<?, ?> build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
            assertFalse(BuildHelper.isFix(build));
            
            project.getBuildersList().removeAll(MockBuilder.class);
            build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
            assertFalse(BuildHelper.isFix(build));
        }
    }
    
    public void testGetResultDescription() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        AbstractBuild<?, ?> build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
        assertEquals("SUCCESS", BuildHelper.getResultDescription(build));
        
        project.getBuildersList().add(new FailureBuilder());
        build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
        assertEquals("FAILURE", BuildHelper.getResultDescription(build));
        
        build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
        assertEquals("STILL FAILING", BuildHelper.getResultDescription(build));
        
        project.getBuildersList().removeAll(FailureBuilder.class);
        build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
        assertEquals("FIXED", BuildHelper.getResultDescription(build));
        
        project.getBuildersList().add(new MockBuilder(Result.UNSTABLE));
        build = project.scheduleBuild2(0).get(120, TimeUnit.SECONDS);
        assertEquals("UNSTABLE", BuildHelper.getResultDescription(build));
        
        build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
        assertEquals("STILL UNSTABLE", BuildHelper.getResultDescription(build));
        
        project.getBuildersList().removeAll(MockBuilder.class);
        build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
        assertEquals("FIXED", BuildHelper.getResultDescription(build));
        
        
        project.getBuildersList().add(new MockBuilder(Result.ABORTED));
        build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
        assertEquals("ABORTED", BuildHelper.getResultDescription(build));
        
        project.getBuildersList().removeAll(MockBuilder.class);
        // don't mark this as FIXED:
        build = project.scheduleBuild2(0).get(10, TimeUnit.SECONDS);
        assertEquals("SUCCESS", BuildHelper.getResultDescription(build));
    }
}
