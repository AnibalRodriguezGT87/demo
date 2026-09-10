package com.csv;

import com.exception.ReaderException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
@StepScope
public class CsvLineReader extends AbstractItemCountingItemStreamItemReader<String> {

    @Value("#{jobParameters['fileNameInput']}")
    private String fileName;
    private BufferedReader reader;

    public CsvLineReader() {
        setName("data-reader");
    }

    @Override
    protected void doOpen() throws ReaderException {
        try {
            Resource resource = new ClassPathResource(fileName);
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            int linesToSkip = 1;
            for (int i = 0; i < linesToSkip; i++) {
                reader.readLine();
            }
        } catch (Exception e) {
            throw new ReaderException("Error occurred while opening CSV file: " + e.getMessage(), e);
        }
    }

    @Override
    protected String doRead() throws ReaderException {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new ReaderException("Error occurred while reading from CSV file: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doClose() throws ReaderException {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            throw new ReaderException("Error occurred while closing CSV file: " + e.getMessage(), e);
        }
    }

    @Override
    protected void jumpToItem(int itemIndex) throws ReaderException {
        try {
            if (reader == null) {
                return;
            }

            for (int i = 0; i < itemIndex; i++) {
                if (reader.readLine() == null) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new ReaderException("Error occurred while jumping to item in CSV file: " + e.getMessage(), e);
        }
    }
}