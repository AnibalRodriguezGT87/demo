package com.config;

import com.job.Iso8583MessageListener;
import com.job.Iso8583MessageProcessor;
import com.job.Iso8583MessageReader;
import com.job.Iso8583MessageWriter;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JobBatchConfigurationTest {

    private final JobBatchConfiguration config = new JobBatchConfiguration();

    @Test
    void jobMaking_buildsJobWithName() {
        Step step = mock(Step.class);
        JobRepository jobRepo = mock(JobRepository.class);
        Job job = config.jobMaking(step, jobRepo);
        assertNotNull(job);
        assertEquals(JobBatchConfiguration.JOB_BEAN_NAME, job.getName());
    }

    @Test
    void step_buildsStepWithName() {
        Iso8583MessageReader reader = mock(Iso8583MessageReader.class);
        Iso8583MessageProcessor processor = mock(Iso8583MessageProcessor.class);
        Iso8583MessageListener listener = mock(Iso8583MessageListener.class);
        Iso8583MessageWriter writer = mock(Iso8583MessageWriter.class);
        JobRepository jobRepo = mock(JobRepository.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

        Step step = config.step(processor, listener, writer, reader, jobRepo, transactionManager);
        assertNotNull(step);
        assertEquals("making-step", step.getName());
    }
}
