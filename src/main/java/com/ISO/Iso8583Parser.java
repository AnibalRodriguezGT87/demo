package com.ISO;

import com.exception.IsoExcepttion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.ISO.Constants.BITMAP_INDEX;
import static com.ISO.Constants.MTI_LENGTH;
import static com.ISO.Constants.FIXED_LENGTH_FIELDS;


/**
 * Iso8583Parser class is responsible for parsing ISO 8583 messages.
 * It extracts the Message Type Indicator (MTI), primary and secondary bitmaps,
 * and data elements from the provided message string.
 */
public class Iso8583Parser {

    /**
     * Parses the given ISO 8583 message string and returns an IsoMessage object.
     *
     * @param decodeMessage the ISO 8583 message string to be parsed
     * @return an IsoMessage object containing the parsed MTI, bitmaps, and data elements
     */
    public IsoMessage parse(String decodeMessage)  throws IsoExcepttion {

        String bitmapBinary;
        String primaryBitmap;
        String secondaryBitmap = "";
        try {
            ParseContext parseContext = new ParseContext(decodeMessage);

            String mti = parseContext.read(MTI_LENGTH);

            primaryBitmap = parseContext.read(BITMAP_INDEX);
            bitmapBinary = parseContext.hexToBinary(primaryBitmap);

            // secondary bitmap
            if (bitmapBinary.charAt(0) == '1') {
                secondaryBitmap = parseContext.read(BITMAP_INDEX);
                bitmapBinary += parseContext.hexToBinary(secondaryBitmap);
            }

            Map<Integer, String> dataElements = new LinkedHashMap<>();
            getDataElements(decodeMessage, bitmapBinary, parseContext.getIndex(), dataElements);
            parseContext.logtMTI(mti);
            return new IsoMessage(mti, primaryBitmap, secondaryBitmap, dataElements);
        } catch (Exception e) {
            throw new IsoExcepttion(e.getMessage());
        }

    }

    /**
     * Extracts data elements from the given ISO 8583 message string based on the provided bitmap.
     *
     * @param decodeMessage the ISO 8583 message string
     * @param bitmapBinary  the binary representation of the bitmap
     * @param index         the starting index for reading data elements
     * @param dataElements  a map to store the extracted data elements
     */
    public void getDataElements(String decodeMessage, String bitmapBinary,int index, Map<Integer, String> dataElements) throws IsoExcepttion {
        try {
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
        } catch (Exception e) {
            throw new IsoExcepttion(e.getMessage());
        }
    }

}