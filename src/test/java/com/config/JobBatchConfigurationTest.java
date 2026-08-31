package com.config;

import com.job.IsoMessageProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JobBatchConfigurationTest {

    private final JobBatchConfiguration config = new JobBatchConfiguration();

    @Test
    void read_createsReaderWithExpectedName() {
        FlatFileItemReader<String> reader = config.read();
        assertNotNull(reader);
        assertEquals("data-reader", reader.getName());
    }

    @Test
    void write_createsWriterWithExpectedName() {
        FlatFileItemWriter<String> writer = config.write();
        assertNotNull(writer);
        assertEquals("output-writer", writer.getName());
    }

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
        FlatFileItemReader<String> reader = mock(FlatFileItemReader.class);
        IsoMessageProcessor processor = mock(IsoMessageProcessor.class);
        FlatFileItemWriter<String> writer = mock(FlatFileItemWriter.class);
        JobRepository jobRepo = mock(JobRepository.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);

        Step step = config.step(reader, processor, writer, jobRepo, tx);
        assertNotNull(step);
        assertEquals("making-step", step.getName());
    }
}
