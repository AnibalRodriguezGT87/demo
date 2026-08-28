package com.ISO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Iso8583ParserTest {

    @Test
    void parse_shouldExtractPrimaryBitmapFields() {
        String input = "010042000400000000021612345678901234560609173030123109789ABC1000123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

        IsoMessage message = new Iso8583Parser().parse(input);

        assertEquals("0100", message.getMti());
        assertEquals("4200040000000002", message.getPrimaryBitmap());
        assertEquals("", message.getSecondaryBitmap());
        assertEquals("1234567890123456", message.getDataElements().get(2));
        assertEquals("0609173030", message.getDataElements().get(7));
        assertEquals("123", message.getDataElements().get(22));
        assertTrue(message.getDataElements().get(63).contains("789ABC"));
    }

    @Test
    void parse_shouldExtractSecondaryBitmapFields() {
        String input = "0100C200000000000000000000000000000216123456789012345606091730301231234567890123456789";

        IsoMessage message = new Iso8583Parser().parse(input);

        assertEquals("0100", message.getMti());
        assertEquals("C200000000000000", message.getPrimaryBitmap());
        assertEquals("0000000000000002", message.getSecondaryBitmap());
        assertEquals("1234567890123456", message.getDataElements().get(2));
        assertEquals("0609173030", message.getDataElements().get(7));
        assertEquals("1234567890123456789", message.getDataElements().get(127));
    }
}
