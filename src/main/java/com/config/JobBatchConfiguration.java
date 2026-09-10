package com.config;

import com.csv.CsvLineReader;
import com.csv.CsvLineWriter;
import com.job.IsoMessageProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
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

    /**
     * This method defines a Job bean that represents the batch job.
     * It takes a Step and a JobRepository as parameters and configures the job with a name, starting step, and an incrementer.
     *
     * @param step the Step to be executed in the job
     * @param jobRepo the JobRepository for managing job metadata
     * @return a Job instance
    */
    @Bean
    public Job jobMaking(Step step,JobRepository jobRepo) {
         return new JobBuilder("making-job", jobRepo )
                .start(step)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    /**
     * This method defines a Step bean that represents a step in the batch job.
     * It takes a FlatFileItemReader, TestProcessor, FlatFileItemWriter, JobRepository, and PlatformTransactionManager as parameters.
     * It configures the step with a name, chunk size, reader, processor, and writer.
     *
     * @param read the FlatFileItemReader for reading data
     * @param isoMessageProcessor the TestProcessor for processing data
     * @param write the FlatFileItemWriter for writing data
     * @param jobRepo the JobRepository for managing job metadata
     * @param transactionManager the PlatformTransactionManager for managing transactions
     * @return a Step instance
    */
    @Bean
    public Step step(CsvLineReader read,
                     IsoMessageProcessor isoMessageProcessor,
                     CsvLineWriter write,
                     JobRepository jobRepo,
                     PlatformTransactionManager transactionManager) {
        return new StepBuilder("making-step", jobRepo)
                .<String, String>chunk(2, transactionManager)
                .reader(read)
                .processor(isoMessageProcessor)
                .writer(write)
                .build();
    }


}
