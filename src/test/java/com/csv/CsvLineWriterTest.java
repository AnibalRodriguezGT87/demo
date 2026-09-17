package com.csv;

import com.exception.ReaderException;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class CsvLineWriterTest {

    @Test
    void write_writesItemsToFileAndCreatesParentDirectories() throws Exception {
        Path outputFile = Files.createTempDirectory("csv-writer-test").resolve("nested").resolve("output.csv");
        CsvLineWriter writer = new CsvLineWriter();
        ReflectionTestUtils.setField(writer, "fileName", outputFile.toString());

        writer.open(new ExecutionContext());
        writer.write(new Chunk<>(List.of("alpha", "beta")));
        writer.close();

        String expected = String.join(System.lineSeparator(), "alpha", "beta") + System.lineSeparator();
        assertEquals(expected, Files.readString(outputFile));
    }

    @Test
    void open_throwsReaderException_whenPathIsDirectory() throws Exception {
        CsvLineWriter writer = new CsvLineWriter();
        Path directory = Files.createTempDirectory("csv-writer-dir");
        ReflectionTestUtils.setField(writer, "fileName", directory.toString());

        ReaderException exception = assertThrows(ReaderException.class, () -> writer.open(new ExecutionContext()));
        assertTrue(exception.getMessage().contains("Error opening file"));
    }

    @Test
    void write_throwsReaderException_whenWriterFails() throws Exception {
        CsvLineWriter writer = new CsvLineWriter();
        BufferedWriter failingWriter = mock(BufferedWriter.class);
        doThrow(new IOException("write failed")).when(failingWriter).write("alpha");
        ReflectionTestUtils.setField(writer, "writer", failingWriter);

        ReaderException exception = assertThrows(ReaderException.class,
                () -> writer.write(new Chunk<>(List.of("alpha"))));

        assertTrue(exception.getMessage().contains("Error writing to file"));
    }
}
