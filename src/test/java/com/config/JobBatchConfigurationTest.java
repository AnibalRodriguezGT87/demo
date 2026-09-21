package com.config;

import com.job.Iso8583MessageProcessor;
import com.job.Iso8583MessageReader;
import com.job.Iso8583MessageWriter;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
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
        assertEquals("making-job", job.getName());
    }

    @Test
    void step_buildsStepWithName() {
        Iso8583MessageReader reader = mock(Iso8583MessageReader.class);
        Iso8583MessageProcessor processor = mock(Iso8583MessageProcessor.class);
        Iso8583MessageWriter writer = mock(Iso8583MessageWriter.class);
        JobRepository jobRepo = mock(JobRepository.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);

        Step step = config.step(reader, processor, writer, jobRepo, tx);
        assertNotNull(step);
        assertEquals("making-step", step.getName());
    }
}
