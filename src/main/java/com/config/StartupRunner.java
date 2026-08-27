package com.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {

    @Autowired
    private final JobLauncher jobLauncher;

    @Qualifier("jobMaking")
    @Autowired
    private final Job jobMaking;

    public StartupRunner(JobLauncher jobLauncher,
                         Job jobMaking) {
        this.jobLauncher = jobLauncher;
        this.jobMaking = jobMaking;
    }

    @Override
    public void run(String... args) throws Exception {
        JobParametersBuilder builder = new JobParametersBuilder();
        for (String arg : args) {
            String[] parts = arg.split("=", 2);
            if (parts.length == 2) {
                builder.addString(parts[0], parts[1]);
            }
        }
        System.out.println("Job started with parameters: " + builder);

        jobLauncher.run(jobMaking, builder.toJobParameters());
    }
}