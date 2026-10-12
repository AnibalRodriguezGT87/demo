package com.job;

import com.exception.IsoException;
import com.iso.Iso8583Parser;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Iso8583MessageProcessor is a Spring Batch ItemProcessor that processes ISO 8583 messages.
 * It reads and parses the input ISO 8583 message, extracts relevant fields, and returns a string representation of the parsed message.
 * The processor also implements StepExecutionListener to provide additional functionality during step execution.
 */
@Component
@Slf4j
public class Iso8583MessageProcessor implements ItemProcessor<BufferedReader, String> {

    /**
     * Processes an input item (ISO 8583 message) and returns a string representation of the parsed message.
     *
     * @param reader the input item to be processed
     * @return a string representation of the parsed ISO 8583 message
     */
    @Override
    public String process(@Nonnull BufferedReader reader) {
        try {
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                try {
                    String processed = processLine(line);
                    if (processed != null) {
                        output.append(processed).append('\n');
                    }
                } catch (Exception e) {
                    log.error("Error procesando línea: {}", line, e);
                }
            }
            reader.close();
            return output.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String processLine(String line) {
        try {
            Iso8583Parser iso8583Parser = new Iso8583Parser();
            return iso8583Parser.parse(line).toString();
        } catch (IsoException e) {
            log.error("Error occurred while processing ISO 8583 message: {}", e.getMessage(), e);
            return null;
        }
    }
}
