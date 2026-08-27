package com.job;

import jakarta.annotation.Nonnull;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Component
public class TestWriter implements ItemWriter<String> {

    @Override
    public void write(@Nonnull Chunk<? extends String> chunk) {
        String fileLocation = "src/main/resources/output.txt";
        new FlatFileItemWriterBuilder<String>()
                .name("output-writer")
                .resource(new FileSystemResource(fileLocation))
                .lineAggregator(item -> item)
                .build();
    }
}
