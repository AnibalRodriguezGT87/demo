package com.csv;

import com.exception.ReaderException;
import jakarta.annotation.Nonnull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * CsvLineWriter is a Spring Batch ItemStreamWriter that writes lines of text to a CSV file.
 * It uses a BufferedWriter to write data to the specified output file, which is provided as a job parameter.
 * The writer is step-scoped, meaning it is created and managed within the context of a specific step execution.
 */
@Component
@StepScope
public class CsvLineWriter implements ItemStreamWriter<String> {

    @Value("#{jobParameters['fileNameOutput']}")
    private String fileName;
    private BufferedWriter writer;

    @Override
    public void write(Chunk<? extends String> chunk) throws ReaderException {
        try {
            for (String item : chunk.getItems()) {
                writer.write(item);
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            throw new ReaderException("Error writing to file: " + fileName, e);
        }
    }

    @Override
    public void open(@Nonnull ExecutionContext executionContext) throws ReaderException {
        try {
            Path path = Paths.get(fileName);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            throw new ReaderException("Error opening file: " + fileName, e);
        }
    }

    @Override
    public void close() throws ReaderException {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            throw new ReaderException("Error closing file", e);
        }
    }
}