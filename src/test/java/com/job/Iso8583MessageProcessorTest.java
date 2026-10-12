package com.job;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Iso8583MessageProcessorTest {

    @Test
    void process_withOnlyPrimaryBitmap_returnsParsedMessageString() {
        Iso8583MessageProcessor processor = new Iso8583MessageProcessor();
        String input = "0100"
                + "4200000000000000"
                + "16" + "1234567890123456"
                + "0609173030";

        String result = processor.processLine(input);

        assertNotNull(result);
        assertTrue(result.contains("mti='0100'"));
        assertTrue(result.contains("primaryBitmap='4200000000000000'"));
        assertTrue(result.contains("2=1234567890123456"));
        assertTrue(result.contains("7=0609173030"));
    }

    @Test
    void process_withSecondBitmap_returnsParsedMessageString() {
        Iso8583MessageProcessor processor = new Iso8583MessageProcessor();
        String pan = "1234567890123456";
        String de127 = "1234567890123456789";

        String input = "0100"
                + "C000000000000000"
                + "0000000000000002"
                + "16" + pan
                + String.format("%03d", de127.length()) + de127;

        String result = processor.processLine(input);

        assertNotNull(result);
        assertTrue(result.contains("mti='0100'"));
        assertTrue(result.contains("primaryBitmap='C000000000000000'"));
        assertTrue(result.contains("secondaryBitmap='0000000000000002'"));
        assertTrue(result.contains("2=1234567890123456"));
        assertTrue(result.contains("127=1234567890123456789"));
    }

    @Test
    void process_withMalformedMessage_returnsNull() {
        Iso8583MessageProcessor processor = new Iso8583MessageProcessor();

        assertNull(processor.processLine("malformed"));
    }

    @Test
    void process_readsEachLine_skipsMalformedMessagesAndClosesReader() {
        Iso8583MessageProcessor processor = new Iso8583MessageProcessor();
        String input = "0100"
                + "4200000000000000"
                + "16" + "1234567890123456"
                + "0609173030";
        BufferedReader reader = new BufferedReader(
                new StringReader(input + "\nmalformed\n" + input));

        String result = processor.process(reader);

        assertNotNull(result);
        assertEquals(2, result.split("mti='0100'", -1).length - 1);
        assertTrue(result.endsWith("\n"));
        assertThrows(IOException.class, reader::readLine);
    }
}
