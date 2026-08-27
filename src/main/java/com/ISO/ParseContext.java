package com.ISO;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class ParseContext {

    private final String message;
    private int index;

    public ParseContext(String message) {
        this.message = message;
    }

    public String read(int length) {
        String value = message.substring(index, index + length);
        index += length;
        return value;
    }


    // Convert Hexa to a Binario
    public String hexToBinary(String hex) {
        StringBuilder sb = new StringBuilder();
        for (char c : hex.toCharArray()) {
            sb.append(
                    String.format("%4s",
                            Integer.toBinaryString(Integer.parseInt(String.valueOf(c), 16))).replace(' ', '0'));
        }

        return sb.toString();
    }

    public void logtMTI(String mti) {
        log.info("MTI Log:");
        log.info("{} - {}", mti.charAt(0), getVersion(mti.charAt(0)));
        log.info("{} - {}", mti.charAt(1), getMessageClass(mti.charAt(1)));
        log.info("{} - {}", mti.charAt(2), getMessageFunction(mti.charAt(2)));
        log.info("{} - {}", mti.charAt(3), getService(mti.charAt(3)));
    }

    private String getVersion(char c){
        return switch (c) {
            case '0' -> "ISO 8583 version: 1987";
            case '1' -> "ISO 8583 version: 1993";
            case '2' -> "ISO 8583 version: 2003";
            default -> "Unknown";
        };
    }

    private String getMessageClass(char c) {
        return switch (c) {
            case '1' -> "Authorization";
            case '2' -> "Financial";
            case '3' -> "File update";
            case '4' -> "Reversal";
            case '5' -> "Reconciliation";
            case '6' -> "Admin";
            case '8' -> "Network management";
            default -> "Unknown";
        };
    }

    private String getMessageFunction(char c) {
        return  switch (c) {
            case '0' -> "Request";
            case '1' -> "Response";
            case '2' -> "Advice";
            case '3' -> "Advice Response";
            default -> "Unknown";
        };
    }

    private String getService(char c) {
        return switch (c) {
            case '0' ->"Acquirer";
            case '1' -> "Issuer";
            default -> "Unknown";
        };
    }

}