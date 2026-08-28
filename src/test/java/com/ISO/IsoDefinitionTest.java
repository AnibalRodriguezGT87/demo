package com.ISO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IsoDefinitionTest {

    @Test
    void constructor_setsFieldsForLLVAR() {
        IsoDefinition def = new IsoDefinition(IsoTypeDataElementEnum.LLVAR, 10, "FieldA");
        assertEquals(IsoTypeDataElementEnum.LLVAR, def.getTypeDataElement());
        assertEquals(10, def.getLengthDefinition());
        assertEquals("FieldA", def.getName());
        assertEquals(2, def.getTypeElementSize());
    }

    @Test
    void constructor_setsFieldsForLLLVAR() {
        IsoDefinition def = new IsoDefinition(IsoTypeDataElementEnum.LLLVAR, 20, "FieldB");
        assertEquals(IsoTypeDataElementEnum.LLLVAR, def.getTypeDataElement());
        assertEquals(20, def.getLengthDefinition());
        assertEquals("FieldB", def.getName());
        assertEquals(3, def.getTypeElementSize());
    }

    @Test
    void constructor_setsFieldsForFIXED() {
        IsoDefinition def = new IsoDefinition(IsoTypeDataElementEnum.FIXED, 5, "FieldC");
        assertEquals(IsoTypeDataElementEnum.FIXED, def.getTypeDataElement());
        assertEquals(5, def.getLengthDefinition());
        assertEquals("FieldC", def.getName());
        assertEquals(0, def.getTypeElementSize());
    }
}
