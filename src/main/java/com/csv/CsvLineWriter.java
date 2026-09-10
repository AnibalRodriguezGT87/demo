package com.csv;

import jakarta.annotation.Nonnull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
@StepScope
public class CsvLineWriter implements ItemStreamWriter<String> {

    @Value("#{jobParameters['fileNameOutput']}")
    private String fileName;
    private BufferedWriter writer;

    @Override
    public void write(Chunk<? extends String> chunk) throws ItemStreamException {
        try {
            for (String item : chunk.getItems()) {
                writer.write(item);
                writer.newLine();
            }

            writer.flush();
        } catch (IOException e) {
            throw new ItemStreamException("Error writing to file: " + fileName, e);
        }
    }

    @Override
    public void open(@Nonnull ExecutionContext executionContext) throws ItemStreamException {
        try {
            Path path = Paths.get(fileName);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            throw new ItemStreamException("Error opening file: " + fileName, e);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            throw new ItemStreamException("Error closing file", e);
        }
    }
}