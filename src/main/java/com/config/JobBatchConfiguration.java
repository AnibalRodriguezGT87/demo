package com.config;

import com.job.Iso8583MessageListener;
import com.job.Iso8583MessageProcessor;
import com.job.Iso8583MessageReader;
import com.job.Iso8583MessageWriter;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * JobBatchConfiguration class is a configuration class for Spring Batch jobs.
 * It defines beans for reading from a CSV file, processing the data, and writing to an output file.
 * It also configures a job and a step for the batch processing.
**/
@Configuration
public class JobBatchConfiguration {

    public static final String JOB_BEAN_NAME = "jobMaking";
    /**
     * This method defines a Job bean that represents the batch job.
     * It takes a Step and a JobRepository as parameters and configures the job with a name, starting step, and an incrementer.
     *
     * @param step the Step to be executed in the job
     * @param jobRepo the JobRepository for managing job metadata
     * @return a Job instance
    */
    @Bean(name = JOB_BEAN_NAME)
    public Job jobMaking(Step step, JobRepository jobRepo) {
         return new JobBuilder(JOB_BEAN_NAME, jobRepo )
                .start(step)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    /**
     * This method defines a Step bean that represents a step in the batch job.
     * It takes an Iso8583MessageReader, Iso8583MessageProcessor, Iso8583MessageWriter, and JobRepository as parameters.
     * It configures the step with a name, chunk size, reader, processor, and writer.
     *
     * @param read the Iso8583MessageReader for reading data
     * @param processor the Iso8583MessageProcessor for processing data
     * @param write the Iso8583MessageWriter for writing data
     * @param jobRepo the JobRepository for managing job metadata
     * @return a Step instance
    */
    @Bean
    public Step step(Iso8583MessageProcessor processor,
                     Iso8583MessageListener listener,
                     Iso8583MessageWriter write,
                     Iso8583MessageReader read,
                     JobRepository jobRepo,
                     PlatformTransactionManager transactionManager) {
        return new StepBuilder("making-step", jobRepo)
                .<String, String>chunk(2)
                .reader(read)
                .processor(processor)
                .writer(write)
                .listener(listener)
                .transactionManager(transactionManager)
                .build();
    }

}
