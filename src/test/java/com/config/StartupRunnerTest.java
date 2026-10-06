package com.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StartupRunnerTest {

    @Test
    void run_withArgs_callsLauncherWithParsedParameters() throws Exception {
        JobOperator launcher = mock(JobOperator.class);
        ApplicationContext context = mock(ApplicationContext.class);
        Job job = mock(Job.class);
        JobExecution exec = mock(JobExecution.class);
        when(launcher.start(any(Job.class), any(JobParameters.class))).thenReturn(exec);
        when(context.getBean("job", Job.class)).thenReturn(job);

        StartupRunner runner = new StartupRunner();
        ReflectionTestUtils.setField(runner, "jobOperator", launcher);
        ReflectionTestUtils.setField(runner, "context", context);
        ReflectionTestUtils.setField(runner, "jobName", "job");
        runner.run("foo=bar", "baz=qux");

        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        verify(launcher).start(eq(job), captor.capture());
        JobParameters params = captor.getValue();

        assertEquals("bar", params.getString("foo"));
        assertEquals("qux", params.getString("baz"));
        assertEquals("/upload", params.getString("remoteDirectory"));
        assertEquals("output.csv", params.getString("fileNameOutput"));
        assertEquals("data.csv", params.getString("fileNameInput"));
    }

    @Test
    void run_withInvalidArgs_ignoresNonKeyValueAndStillLaunches() throws Exception {
        JobOperator launcher = mock(JobOperator.class);
        ApplicationContext context = mock(ApplicationContext.class);
        Job job = mock(Job.class);
        JobExecution exec = mock(JobExecution.class);
        when(launcher.start(any(Job.class), any(JobParameters.class))).thenReturn(exec);
        when(context.getBean("job", Job.class)).thenReturn(job);

        StartupRunner runner = new StartupRunner();
        ReflectionTestUtils.setField(runner, "jobOperator", launcher);
        ReflectionTestUtils.setField(runner, "context", context);
        ReflectionTestUtils.setField(runner, "jobName", "job");
        runner.run("invalidArg", "onlykey=");

        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        verify(launcher).start(eq(job), captor.capture());
        JobParameters params = captor.getValue();

        assertEquals("", params.getString("onlykey"));
        assertNull(params.getString("invalidArg"));
    }
}
