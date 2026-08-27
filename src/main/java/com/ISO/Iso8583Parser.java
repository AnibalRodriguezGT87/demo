package com.ISO;

import java.util.*;

import static com.ISO.Constants.FIXED_LENGTH_FIELDS;
import static com.ISO.Constants.MTI_LENGTH;

public class Iso8583Parser {

    private final int BITMAP_INDEX = 16;

    public IsoMessage parse(String decodeMessage) {
        String bitmapBinary;
        String primaryBitmap;
        String secondaryBitmap = "";

        ParseContext parseContext = new ParseContext(decodeMessage);

        String mti = parseContext.read(MTI_LENGTH);

        primaryBitmap = parseContext.read(BITMAP_INDEX);
        bitmapBinary = parseContext.hexToBinary(primaryBitmap);

        // secondary bitmap
        if (bitmapBinary.charAt(0) == '1') {
            secondaryBitmap = parseContext.read(BITMAP_INDEX);
            bitmapBinary += parseContext. hexToBinary(secondaryBitmap);
        }

        Map<Integer, String> dataElements = new LinkedHashMap<>();
        getDataElements(decodeMessage, bitmapBinary, parseContext.getIndex(), dataElements);
        parseContext.logtMTI(mti);

        return new IsoMessage(mti, primaryBitmap, secondaryBitmap, dataElements);
    }

    public void getDataElements(String decodeMessage, String bitmapBinary,int index, Map<Integer, String> dataElements) {
        for (int field = 2; field <= bitmapBinary.length(); field++) {

            if (bitmapBinary.charAt(field - 1) == '1') {

                IsoDefinition size = FIXED_LENGTH_FIELDS.get(field);

                 if (Objects.nonNull(size) && !size.getTypeDataElement().equals(IsoTypeDataElementEnum.FIXED)) {
                     //get size of a DE
                    int sizeElement = Integer.parseInt(decodeMessage.substring(index, index + size.getTypeElementSize()));
                    int finalSize = index + size.getTypeElementSize() + sizeElement;
                    String value = decodeMessage.substring(index + size.getTypeElementSize(), Math.min(finalSize, decodeMessage.length()));
                    dataElements.put(field, value);
                    index += size.getTypeElementSize() + size.getLengthDefinition();
                 } else {
                    String value = decodeMessage.substring(index , index +  size.getLengthDefinition());
                    dataElements.put(field, value);
                    index += size.getLengthDefinition();
                }
            }
      }
    }

}