package com.job;

import com.iso.Iso8583Parser;
import com.exception.IsoException;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.core.JobParameters;
//import org.springframework.batch.core.StepExecution;
import org.cccc_online.commons.fixedlength.builder.FileSchemaBuilder;
import org.cccc_online.commons.fixedlength.model.FileSchema;
import org.cccc_online.commons.fixedlength.model.RecordData;
import org.cccc_online.commons.fixedlength.reader.FixedLengthReader;
import org.cccc_online.commons.fixedlength.reader.StandardFixedLengthReader;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * TestProcessor class implements the ItemProcessor interface to process ISO 8583 messages.
 * It also implements StepExecutionListener to listen to step execution events.
 */
@Component
@Slf4j
public class IsoMessageProcessor implements ItemProcessor<String, String>, StepExecutionListener {

   /* private JobParameters jobParameters;
    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.jobParameters = stepExecution.getJobParameters();
    }*/

    /**
     * Processes an input item (ISO 8583 message) and returns a string representation of the parsed message.
     *
     * @param item the input item to be processed
     * @return a string representation of the parsed ISO 8583 message
     */
    @Override
    public String process(@Nonnull String item) {
        //String fileName = jobParameters.getString("fileName");
        try {
            Iso8583Parser iso8583Parser = new Iso8583Parser();
            return iso8583Parser.parse(item).toString();
        } catch (IsoException e) {
            log.error("Error occurred while processing ISO 8583 message: {}", e.getMessage(), e);
            return null;
        }
    }
}
