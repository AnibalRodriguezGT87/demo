package com.ISO;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * IsoMessage class represents an ISO 8583 message.
 * It includes the Message Type Indicator (MTI), primary and secondary bitmaps,
 * and a map of data elements.
 */
@Getter
@Setter
public class IsoMessage {

    private String mti;
    private String primaryBitmap;
    private String secondaryBitmap;
    private Map<Integer, String> dataElements;

    public IsoMessage(String mti,
                      String primaryBitmap,
                      String secondaryBitmap,
                      Map<Integer, String> dataElements) {
        this.mti = mti;
        this.primaryBitmap = primaryBitmap;
        this.secondaryBitmap = secondaryBitmap;
        this.dataElements = dataElements;
    }

    /**
     * Returns a string representation of the IsoMessage object.
     *
     * @return a string containing the MTI, primary bitmap, secondary bitmap, and data elements
     */
    @Override
    public String toString() {
        return "IsoMessage{" +
                "mti='" + getMti() + '\'' +
                ", primaryBitmap='" + getPrimaryBitmap() + '\'' +
                ", secondaryBitmap='" + getSecondaryBitmap() + '\'' +
                ", dataElements=" + getDataElements() +
                '}';
    }
}