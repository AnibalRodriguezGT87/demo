package com.csv;

import jakarta.annotation.Nonnull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
    import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * CsvLineReader is a Spring Batch ItemStreamReader that reads lines of text from a CSV file.
 * It uses a BufferedReader to read data from the specified input file, which is provided as a job parameter.
 * The reader is step-scoped, meaning it is created and managed within the context of a specific step execution.
 */
@Component
@StepScope
public class CsvLineReader implements ItemStreamReader<String> {

    @Value("#{jobParameters['fileNameInput']}")
    private String fileName;
    private BufferedReader reader;

    public CsvLineReader() {
    }

    @Override
    public void open(@Nonnull ExecutionContext executionContext) throws ItemStreamException {
        try {
            Resource resource = new ClassPathResource(fileName);
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            int linesToSkip = 1;
            for (int i = 0; i < linesToSkip; i++) {
                reader.readLine();
            }
        } catch (Exception e) {
            throw new ItemStreamException("Error occurred while opening CSV file: " + e.getMessage(), e);
        }
    }

    @Override
    public String read() throws ItemStreamException {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new ItemStreamException("Error occurred while reading from CSV file: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            throw new ItemStreamException("Error occurred while closing CSV file: " + e.getMessage(), e);
        }
    }

}