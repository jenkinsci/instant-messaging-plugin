package hudson.plugins.im.bot;

import hudson.model.AbstractProject;
import hudson.model.BooleanParameterDefinition;
import hudson.model.BooleanParameterValue;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.RunParameterDefinition;
import hudson.model.RunParameterValue;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;

import hudson.plugins.im.Sender;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

class BuildCommandTest {

    private FreeStyleProject project;
    private List<ParameterValue> parsedParameters;

    @Test
    void testDelay() {
        Bot bot = mock(Bot.class);
        when(bot.getImId()).thenReturn("hudsonbot");

        BuildCommand cmd = new BuildCommand();
        JobProvider jobProvider = mock(JobProvider.class);
        cmd.setJobProvider(jobProvider);

        AbstractProject<?, ?> project = mockProject(jobProvider);

        Sender sender = new Sender("sender");

        cmd.getReply(bot, sender, new String[]{ "build", "project", "5s" });
        verify(project).hasPermission(Item.BUILD);
        verify(project).scheduleBuild(eq(5), Mockito.any(Cause.class));

        project = mockProject(jobProvider);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "5" });
        verify(project).hasPermission(Item.BUILD);
        verify(project).scheduleBuild(eq(5), Mockito.any(Cause.class));

        project = mockProject(jobProvider);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "1m" });
        verify(project).hasPermission(Item.BUILD);
        verify(project).scheduleBuild(eq(60), Mockito.any(Cause.class));

        project = mockProject(jobProvider);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "1min" });
        verify(project).hasPermission(Item.BUILD);
        verify(project).scheduleBuild(eq(60), Mockito.any(Cause.class));

        project = mockProject(jobProvider);
        cmd.getReply(bot, sender, new String[]{ "build", "project", "2h" });
        verify(project).hasPermission(Item.BUILD);
        verify(project).scheduleBuild(eq(7200), Mockito.any(Cause.class));

        // TODO kutzi: this doesn't work, yet. Catch typo before 's'
        //Mockito.reset(project);
        //when(project.getQuietPeriod()).thenReturn(42);
        //cmd.getReply("sender", new String[]{ "build", "project", "1as" });
        //verify(project).scheduleBuild(eq(42), (Cause) Mockito.any());
    }

    @SuppressWarnings("unchecked")
    private AbstractProject<?, ?> mockProject(JobProvider jobProvider) {
        @SuppressWarnings("rawtypes")
        AbstractProject project = mock(FreeStyleProject.class);
        when(jobProvider.getJobByNameOrDisplayName(Mockito.anyString())).thenReturn(project);
        when(project.hasPermission(Item.BUILD)).thenReturn(Boolean.TRUE);
        when(project.isBuildable()).thenReturn(true);
        return project;
    }

    @Test
    void parametersFromCommandShouldBePassedToBuild() {
        Bot bot = mock(Bot.class);
        when(bot.getImId()).thenReturn("hudsonbot");

        BuildCommand cmd = new BuildCommand();
        JobProvider jobProvider = mock(JobProvider.class);
        cmd.setJobProvider(jobProvider);

        AbstractProject<?, ?> project = mockProject2(jobProvider);
        project = mockProject(jobProvider);
        when(project.isParameterized()).thenReturn(Boolean.TRUE);
        when(project.getProperty(ParametersDefinitionProperty.class)).thenReturn(
                new ParametersDefinitionProperty(
                        new StringParameterDefinition("key", "default value", ""),
                        new BooleanParameterDefinition("key2", false, "")));

        Sender sender = new Sender("sender");
        cmd.getReply(bot, sender, new String[]{ "build", "project", "3s", "key=value", "key2=true" });

        ArgumentCaptor<ParametersAction> captor = ArgumentCaptor.forClass(ParametersAction.class);
        verify(project).scheduleBuild(anyInt(), any(Cause.class),
                captor.capture());

        assertEquals(2, captor.getValue().getParameters().size());
        assertEquals(new StringParameterValue("key", "value"),
                captor.getValue().getParameters().get(0));
        assertEquals(new BooleanParameterValue("key2", true),
                captor.getValue().getParameters().get(1));
    }

    @Test
    void unknownParametersShouldBeIgnored() {
        // TODO: really? Shouldn't we better raise an error?
        Bot bot = mock(Bot.class);
        when(bot.getImId()).thenReturn("hudsonbot");

        BuildCommand cmd = new BuildCommand();
        JobProvider jobProvider = mock(JobProvider.class);
        cmd.setJobProvider(jobProvider);

        AbstractProject<?, ?> project = mockProject(jobProvider);
        when(project.isParameterized()).thenReturn(Boolean.TRUE);
        when(project.getProperty(ParametersDefinitionProperty.class)).thenReturn(
                new ParametersDefinitionProperty(new StringParameterDefinition("key", "default value", "")));

        Sender sender = new Sender("sender");
        cmd.getReply(bot, sender, new String[]{ "build", "project", "key=value", "unexisting_key=value" });

        ArgumentCaptor<ParametersAction> captor = ArgumentCaptor.forClass(ParametersAction.class);
        verify(project).scheduleBuild(anyInt(), any(Cause.class),
                captor.capture());

        assertEquals(1, captor.getValue().getParameters().size());
        assertEquals(new StringParameterValue("key", "value"),
                captor.getValue().getParameters().get(0));
    }

    @Test
    void shouldParseRunParameter() {
        givenAParametrizedProject().withParameterDefinitions(
                new RunParameterDefinition("run", "projectName", "description")
        );

        whenParametersAreParsed("run=job#123");

        assertEquals(1, parsedParameters.size());
        ParameterValue parameter = parsedParameters.get(0);
        assertInstanceOf(RunParameterValue.class, parameter);
        RunParameterValue passwordParameter = (RunParameterValue) parameter;
        assertEquals("123", passwordParameter.getNumber());
        assertEquals("job", passwordParameter.getJobName());
    }

    @Test
    void shouldTakeDefaultValueOfParameter() {
        givenAParametrizedProject().withParameterDefinitions(
                new StringParameterDefinition("stringParam", "defaultValue", "description")
        );

        whenParametersAreParsed();

        assertEquals(1, parsedParameters.size());
        assertEquals(new StringParameterValue("stringParam", "defaultValue"), parsedParameters.get(0));
    }

    private BuildCommandTest givenAParametrizedProject() {
        this.project = mock(FreeStyleProject.class);
        when(project.isParameterized()).thenReturn(true);
        return this;
    }

    private void withParameterDefinitions(ParameterDefinition... definitions) {
        when(project.getProperty(ParametersDefinitionProperty.class)).thenReturn(
                new ParametersDefinitionProperty(definitions));
    }

    private void whenParametersAreParsed(String... parameters) {
        BuildCommand cmd = new BuildCommand();
        this.parsedParameters = cmd.parseBuildParameters(parameters, project, new StringBuilder());
    }

    @Test
    void disabledProjectShouldNotBeScheduled() {
        Bot bot = mock(Bot.class);
        BuildCommand cmd = new BuildCommand();
        JobProvider jobProvider = mock(JobProvider.class);
        cmd.setJobProvider(jobProvider);

        AbstractProject<?, ?> project = mockProject3(jobProvider);
        when(project.isBuildable()).thenReturn(false);

        Sender sender = new Sender("sender");
        cmd.getReply(bot, sender, new String[]{"build", "project"});

        verify(project, times(0)).scheduleBuild(anyInt(), any(Cause.class));
    }

    private AbstractProject<?, ?> mockProject2(JobProvider jobProvider) {
        @SuppressWarnings("rawtypes")
        AbstractProject project = mock(FreeStyleProject.class);
        return project;
    }

    private AbstractProject<?, ?> mockProject3(JobProvider jobProvider) {
        @SuppressWarnings("rawtypes")
        AbstractProject project = mock(FreeStyleProject.class);
        when(jobProvider.getJobByNameOrDisplayName(Mockito.anyString())).thenReturn(project);
        when(project.hasPermission(Item.BUILD)).thenReturn(Boolean.TRUE);
        return project;
    }
}
