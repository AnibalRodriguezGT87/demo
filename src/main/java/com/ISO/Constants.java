package com.ISO;

import java.util.Map;

public class Constants {

    public static final Map<Integer, IsoDefinition> FIXED_LENGTH_FIELDS = Map.of(
            2, new IsoDefinition(IsoTypeDataElementEnum.LLVAR, 16, "PAN"),
            7, new IsoDefinition(IsoTypeDataElementEnum.FIXED, 10, "DATE"),
            22, new IsoDefinition(IsoTypeDataElementEnum.FIXED, 3, "POS Entry Mode"),
            63, new IsoDefinition(IsoTypeDataElementEnum.LLLVAR, 105 ,"Reserved Private"),
            127, new IsoDefinition(IsoTypeDataElementEnum.LLLVAR, 105, "Private Use")
    );
    public static final int MTI_LENGTH = 4;
}
