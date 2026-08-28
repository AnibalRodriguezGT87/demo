package com.job;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestProcessorTest {

    @Test
    void process_withValidIsoMessage_returnsParsedMessageString() {
        TestProcessor processor = new TestProcessor();
        String input = "010042000400000000021612345678901234560609173030123109789ABC1000123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

        String result = processor.process(input);

        assertNotNull(result);
        assertTrue(result.contains("mti='0100'"));
        assertTrue(result.contains("primaryBitmap='4200040000000002'"));
        assertTrue(result.contains("dataElements={2=1234567890123456"));
        assertTrue(result.contains("7=0609173030"));
    }

    @Test
    void process_withSecondValidIsoMessage_returnsParsedMessageString() {
        TestProcessor processor = new TestProcessor();
        String input = "0100C200000000000000000000000000000216123456789012345606091730301231234567890123456789";

        String result = processor.process(input);

        assertNotNull(result);
        assertTrue(result.contains("mti='0100'"));
        assertTrue(result.contains("primaryBitmap='C200000000000000'"));
        assertTrue(result.contains("secondaryBitmap='0000000000000002'"));
        assertTrue(result.contains("2=1234567890123456"));
        assertTrue(result.contains("127=1234567890123456789"));
    }
}
