package com.csv;

import com.exception.IsoException;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class CsvLineReaderTest {

    @Test
    void openAndRead_skipsHeaderAndReadsAllRows() throws Exception {
        CsvLineReader reader = new CsvLineReader();
        ReflectionTestUtils.setField(reader, "fileName", "csv-reader-sample.csv");

        reader.open(new ExecutionContext());

        assertEquals("1,John", reader.read());
        assertEquals("2,Jane", reader.read());
        assertEquals("3,Peter", reader.read());
        assertNull(reader.read());

        reader.close();
    }

    @Test
    void jumpToItem_skipsRowsFromCurrentPosition() throws Exception {
        CsvLineReader reader = new CsvLineReader();
        ReflectionTestUtils.setField(reader, "fileName", "csv-reader-sample.csv");

        reader.open(new ExecutionContext());
        reader.jumpToItem(1);

        assertEquals("2,Jane", reader.read());

        reader.close();
    }

    @Test
    void open_throwsIsoException_whenFileDoesNotExist() {
        CsvLineReader reader = new CsvLineReader();
        ReflectionTestUtils.setField(reader, "fileName", "missing-file.csv");

        ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(new ExecutionContext()));

        assertTrue(exception.getCause() instanceof IsoException);
        assertTrue(exception.getCause().getMessage().contains("Error occurred while opening CSV file"));
    }

    @Test
    void read_throwsItemStreamException_whenUnderlyingReaderFails() throws Exception {
        CsvLineReader reader = new CsvLineReader();
        BufferedReader failingReader = mock(BufferedReader.class);
        ReflectionTestUtils.setField(reader, "reader", failingReader);

        java.io.IOException ioException = new IOException("boom");
        org.mockito.Mockito.when(failingReader.readLine()).thenThrow(ioException);

        IsoException exception = assertThrows(IsoException.class, reader::read);

        assertTrue(exception.getMessage().contains("Error occurred while reading from CSV file"));
    }

    @Test
    void close_throwsItemStreamException_whenReaderCannotBeClosed() throws Exception {
        CsvLineReader reader = new CsvLineReader();
        BufferedReader failingReader = mock(BufferedReader.class);
        doThrow(new IOException("cannot close")).when(failingReader).close();
        ReflectionTestUtils.setField(reader, "reader", failingReader);

        ItemStreamException exception = assertThrows(ItemStreamException.class, reader::close);

        assertTrue(exception.getCause() instanceof IsoException);
        assertTrue(exception.getCause().getMessage().contains("Error occurred while closing CSV file"));
    }
}
