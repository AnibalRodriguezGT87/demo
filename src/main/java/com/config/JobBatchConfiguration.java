package com.config;

import com.job.TestProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class JobBatchConfiguration {

    @Bean
    public FlatFileItemReader<String> read() {
        return new FlatFileItemReaderBuilder<String>()
                .resource(new ClassPathResource("data.csv"))
                .name("data-reader")
                .linesToSkip(1)
                .lineMapper((line, lineNumber) -> line)
                .build();
    }

    @Bean
    public FlatFileItemWriter<String> write() {
        String fileLocation = "src/main/resources/output.txt";
        return  new FlatFileItemWriterBuilder<String>()
                .name("output-writer")
                .resource(new FileSystemResource(fileLocation))
                .lineAggregator(item -> item)
                .build();
    }

    @Bean
    public Job jobMaking(Step step,JobRepository jobRepo) {
         return new JobBuilder("making-job", jobRepo )
                .start(step)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step step(FlatFileItemReader<String> read,
                     TestProcessor testProcessor,
                     FlatFileItemWriter<String> write,
                     JobRepository jobRepo,
                     PlatformTransactionManager transactionManager) {


        return new StepBuilder("making-step", jobRepo)
                .<String, String>chunk(2, transactionManager)
                .reader(read)
                .processor(testProcessor)
                .writer(write)
                .build();
    }


}
