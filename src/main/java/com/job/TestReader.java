package com.job;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class TestReader implements ItemReader<String> {

    @Override
    public String read() throws Exception {
        return new FlatFileItemReaderBuilder<String>()
                .resource(new ClassPathResource("data.csv"))
                .name("data-reader")
                .linesToSkip(1)
                .lineMapper((line, lineNumber) -> line)
                .build().read();
    }
}
