package com.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Iso8583MessageListener is a Spring Batch StepExecutionListener
 */
@Component
@Slf4j
public class Iso8583MessageListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Iniciando step: {}", stepExecution.getStepName());
        log.info("JobExecution: {}", stepExecution.getJobExecution());
        log.info("ExecutionContext: {}", stepExecution.getExecutionContext());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("Registros leídos: {}", stepExecution.getReadCount());
        log.info("Registros escritos: {}", stepExecution.getWriteCount());

        return stepExecution.getExitStatus();
    }
}
