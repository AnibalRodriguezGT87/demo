package com.iso;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class IsoMessageTest {

    @Test
    void constructor_setsFields_and_toStringContainsValues() {
        Map<Integer, String> data = new HashMap<>();
        data.put(2, "1234567890123456");
        data.put(7, "0609173030");

        IsoMessage msg = new IsoMessage("0100", "4200040000000002", "", data);

        assertEquals("0100", msg.getMti());
        assertEquals("4200040000000002", msg.getPrimaryBitmap());
        assertEquals("", msg.getSecondaryBitmap());
        assertEquals(data, msg.getDataElements());

        String s = msg.toString();
        assertTrue(s.contains("mti='0100'"));
        assertTrue(s.contains("primaryBitmap='4200040000000002'"));
        assertTrue(s.contains("dataElements="));
        assertTrue(s.contains("1234567890123456"));
    }

    @Test
    void setters_updateFields_and_toStringHandlesNulls() {
        IsoMessage msg = new IsoMessage(null, null, null, null);

        msg.setMti("0200");
        msg.setPrimaryBitmap("C200000000000000");
        msg.setSecondaryBitmap("0000000000000002");

        Map<Integer, String> data = new HashMap<>();
        data.put(127, "1234567890123456789");
        msg.setDataElements(data);

        assertEquals("0200", msg.getMti());
        assertEquals("C200000000000000", msg.getPrimaryBitmap());
        assertEquals("0000000000000002", msg.getSecondaryBitmap());
        assertEquals(data, msg.getDataElements());

        String s = msg.toString();
        assertTrue(s.contains("mti='0200'"));
        assertTrue(s.contains("secondaryBitmap='0000000000000002'"));
        assertTrue(s.contains("127=1234567890123456789"));
    }
}
