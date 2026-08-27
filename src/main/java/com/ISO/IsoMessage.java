package com.ISO;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

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