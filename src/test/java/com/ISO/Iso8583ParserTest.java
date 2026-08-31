package com.ISO;

import com.exception.IsoExcepttion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Iso8583ParserTest {

    @Test
    void parse_shouldExtractPrimaryBitmapFields() throws IsoExcepttion {
        Iso8583Parser parser = new Iso8583Parser();
        String pan = "1234567890123456";
        String de7 = "0609173030";

        // MTI + primary bitmap indicating DE2 and DE7 (no secondary)
        String input = "0100" +
                "4200000000000000" + // primary bitmap: bits for DE2 and DE7 set
                "16" + pan + // DE2 LLVAR (length 16)
                de7; // DE7 fixed 10 chars

        IsoMessage message = parser.parse(input);

        assertEquals("0100", message.getMti());
        assertEquals("4200000000000000", message.getPrimaryBitmap());
        assertEquals("", message.getSecondaryBitmap());
        assertEquals(pan, message.getDataElements().get(2));
        assertEquals(de7, message.getDataElements().get(7));
        assertNull(message.getDataElements().get(127));
    }

    @Test
    void parse_shouldExtractSecondaryBitmapFields() throws IsoExcepttion {
        Iso8583Parser parser = new Iso8583Parser();
        String pan = "1234567890123456";
        String de127 = "1234567890123456789"; // 19 chars

        String input = "0100" +
                "C000000000000000" + // primary bitmap: first bit=1 (secondary present) + DE2
                "0000000000000002" + // secondary bitmap: DE127 set (bit 127 -> position 63 in secondary)
                "16" + pan + // DE2 LLVAR
                String.format("%03d", de127.length()) + de127; // DE127 LLLVAR

        IsoMessage message = parser.parse(input);

        assertEquals("0100", message.getMti());
        assertEquals("C000000000000000", message.getPrimaryBitmap());
        assertEquals("0000000000000002", message.getSecondaryBitmap());
        assertEquals(pan, message.getDataElements().get(2));
        assertEquals(de127, message.getDataElements().get(127));
    }

    @Test
    void parse_withMalformedMessage_throwsIsoException() {
        Iso8583Parser parser = new Iso8583Parser();
        // Message too short to contain bitmap -> parsing should fail
        assertThrows(IsoExcepttion.class, () -> parser.parse("0100"));
    }

    @Test
    void parse_withIncorrectLengthForVariableField_throwsIsoException() {
        Iso8583Parser parser = new Iso8583Parser();
        // declare LLVAR length of 16 but provide only 5 chars -> should throw when parsing DE2
        String input = "0100" +
                "4200000000000000" +
                "16" + "12345" +
                "0609173030";
        assertThrows(IsoExcepttion.class, () -> parser.parse(input));
    }
}