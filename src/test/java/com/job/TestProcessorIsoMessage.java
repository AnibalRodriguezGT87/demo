package com.job;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestProcessorIsoMessage {

    @Test
    void process_withOnlyPrimaryBitmap_returnsParsedMessageString() {
        IsoMessageProcessor processor = new IsoMessageProcessor();
        // MTI(4) + primary bitmap(16) only with bits for DE2 and DE7 set -> hex starts with 42
        String input = "0100" +
                "4200000000000000" + // primary bitmap: bits 2 and 7 set
                "16" + "1234567890123456" + // DE2 LLVAR (length 16)
                "0609173030"; // DE7 fixed 10

        String result = processor.process(input);

        assertNotNull(result);
        assertTrue(result.contains("mti='0100'"));
        assertTrue(result.contains("primaryBitmap='4200000000000000'"));
        assertTrue(result.contains("2=1234567890123456"));
        assertTrue(result.contains("7=0609173030"));
    }

    @Test
    void process_withSecondBitmap_returnsParsedMessageString() {
        IsoMessageProcessor processor = new IsoMessageProcessor();
        // MTI + primary bitmap indicating secondary present (first bit 1) and DE2 set -> C0...
        // secondary bitmap sets DE127 (using 0000000000000002 in tests)
        String pan = "1234567890123456";
        String de127 = "1234567890123456789"; // 19 chars example

        String input = "0100" +
                "C000000000000000" + // primary bitmap (secondary present + DE2)
                "0000000000000002" + // secondary bitmap with DE127 set
                "16" + pan + // DE2
                String.format("%03d", de127.length()) + de127; // DE127 LLLVAR

        String result = processor.process(input);

        assertNotNull(result);
        assertTrue(result.contains("mti='0100'"));
        assertTrue(result.contains("primaryBitmap='C000000000000000'"));
        assertTrue(result.contains("secondaryBitmap='0000000000000002'"));
        assertTrue(result.contains("2=1234567890123456"));
        assertTrue(result.contains("127=1234567890123456789"));
    }

}

