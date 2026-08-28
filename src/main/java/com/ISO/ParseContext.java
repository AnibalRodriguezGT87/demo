package com.ISO;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * ParseContext class provides context for parsing an ISO 8583 message.
 * It includes methods for reading data from the message, converting hexadecimal to binary,
 * and logging the Message Type Indicator (MTI) details.
 */
@Getter
@Setter
@Slf4j
public class ParseContext {

    private final String message;
    private int index;

    public ParseContext(String message) {
        this.message = message;
    }

    /**
     * Reads a substring of the specified length from the message starting at the current index.
     * Updates the index to point to the next unread position in the message.
     *
     * @param length the number of characters to read from the message
     * @return a substring of the specified length from the message
     */
    public String read(int length) {
        String value = message.substring(index, index + length);
        index += length;
        return value;
    }

    /**
     * Converts a hexadecimal string to its binary representation.
     *
     * @param hex the hexadecimal string to be converted
     * @return a binary string representation of the hexadecimal input
     */
    public String hexToBinary(String hex) {
        StringBuilder sb = new StringBuilder();
        for (char c : hex.toCharArray()) {
            sb.append(
                    String.format("%4s",
                            Integer.toBinaryString(Integer.parseInt(String.valueOf(c), 16))).replace(' ', '0'));
        }

        return sb.toString();
    }

    /**
     * Logs the details of the Message Type Indicator (MTI) including version, message class,
     * message function, and service based on the MTI characters.
     *
     * @param mti the Message Type Indicator string to be logged
     */
    public void logtMTI(String mti) {
        log.info("MTI Log:");
        log.info("{} - {}", mti.charAt(0), getVersion(mti.charAt(0)));
        log.info("{} - {}", mti.charAt(1), getMessageClass(mti.charAt(1)));
        log.info("{} - {}", mti.charAt(2), getMessageFunction(mti.charAt(2)));
        log.info("{} - {}", mti.charAt(3), getService(mti.charAt(3)));
    }

    /**
     * Returns the ISO 8583 version based on the first character of the MTI.
     *
     * @param c the first character of the MTI
     * @return a string representing the ISO 8583 version
     */
    private String getVersion(char c){
        return switch (c) {
            case '0' -> "ISO 8583 version: 1987";
            case '1' -> "ISO 8583 version: 1993";
            case '2' -> "ISO 8583 version: 2003";
            default -> "Unknown";
        };
    }

    /**
     * Returns the message class based on the second character of the MTI.
     *
     * @param c the second character of the MTI
     * @return a string representing the message class
     */
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

    /**
     * Returns the message function based on the third character of the MTI.
     *
     * @param c the third character of the MTI
     * @return a string representing the message function
     */
    private String getMessageFunction(char c) {
        return  switch (c) {
            case '0' -> "Request";
            case '1' -> "Response";
            case '2' -> "Advice";
            case '3' -> "Advice Response";
            default -> "Unknown";
        };
    }

    /**
     * Returns the service based on the fourth character of the MTI.
     *
     * @param c the fourth character of the MTI
     * @return a string representing the service
     */
    private String getService(char c) {
        return switch (c) {
            case '0' ->"Acquirer";
            case '1' -> "Issuer";
            default -> "Unknown";
        };
    }

}