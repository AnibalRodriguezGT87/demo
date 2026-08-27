package com.job;

import com.ISO.Iso8583Parser;
import com.ISO.IsoMessage;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.core.JobParameters;
//import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestProcessor implements ItemProcessor<String, String>, StepExecutionListener {

   /* private JobParameters jobParameters;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.jobParameters = stepExecution.getJobParameters();
    }*/

    @Override
    public String process(@Nonnull String item) {
        //String fileName = jobParameters.getString("fileName");
        Iso8583Parser iso8583Parser = new Iso8583Parser();
        IsoMessage isoMessage = iso8583Parser.parse(item);
        log.info(isoMessage.toString());

        return isoMessage.toString();
    }
}
