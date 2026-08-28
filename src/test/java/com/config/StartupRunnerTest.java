package com.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StartupRunnerTest {

    @Test
    void run_withArgs_callsLauncherWithParsedParameters() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        JobExecution exec = mock(JobExecution.class);
        when(launcher.run(any(Job.class), any(JobParameters.class))).thenReturn(exec);

        StartupRunner runner = new StartupRunner(launcher, job);
        runner.run("foo=bar", "baz=qux");

        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        verify(launcher).run(eq(job), captor.capture());
        JobParameters params = captor.getValue();

        assertEquals("bar", params.getString("foo"));
        assertEquals("qux", params.getString("baz"));
    }

    @Test
    void run_withInvalidArgs_ignoresNonKeyValueAndStillLaunches() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        JobExecution exec = mock(JobExecution.class);
        when(launcher.run(any(Job.class), any(JobParameters.class))).thenReturn(exec);

        StartupRunner runner = new StartupRunner(launcher, job);
        runner.run("invalidArg", "onlykey=");

        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        verify(launcher).run(eq(job), captor.capture());
        JobParameters params = captor.getValue();

        // invalidArg is ignored; onlykey= should be present with an empty string value
        assertEquals("", params.getString("onlykey"));
        assertNull(params.getString("invalidArg"));
    }
}
