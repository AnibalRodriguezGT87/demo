package com.job;

import com.exception.IsoException;
import com.iso.Iso8583Parser;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
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
public class Iso8583MessageProcessor implements ItemProcessor<String, String>, StepExecutionListener {

    /**
     * Processes an input item (ISO 8583 message) and returns a string representation of the parsed message.
     *
     * @param item the input item to be processed
     * @return a string representation of the parsed ISO 8583 message
     */
    @Override
    public String process(@Nonnull String item) {
        try {

            FileSchema fileSchema = FileSchemaBuilder.create()
                    // Define the positional offset for record type identification (Offset 0, Length 2)
                    .withRecordTypeIdentifier(0, 2)

                    // ------------------------------------------------------------------
                    // 1. File Header Record ("FH")
                    // ------------------------------------------------------------------
                    .addRecord("FH", "File Header Record")
                    .addAlphaNumericField("recordType", 2) // FH
                    .addNumericField("InstitutionNumber", 8) // 00000007
                    .addAlphaNumericField("FieldLabel", 10) // "FXCURRENCY"
                    .addNumericField("ProcessingDate", 8) // YYYYMMDD
                    .addNumericField("SequenceNo", 4) // 0001
                    .addNumericField("LayoutVersion", 4)  // 0001
                    .addNumericField("FXRateCategory", 3) // 001
                    .addNumericField("BaseCurrency", 3) // 376
                    .addAlphaNumericField("RateFormula", 3) // 001
                    // ------------------------------------------------------------------
                    // 2. Record Detail ("RD")
                    // ------------------------------------------------------------------
                    .addRecord("RD", "Record Detail")
                    .addAlphaNumericField("recordType", 2) // RD
                    .addNumericField("EffectiveDate", 8) // YYYYMMDD
                    .addNumericField("Currency", 3) // 840
                    .addNumericField("SalesRate", 16) // Filler for Sales Rate
                    .addNumericField("MiddleRate", 16) // Middle Rate (e.g.,    3.215)
                    .addNumericField("PurchaseRate", 16) // Filler for Purchase Rate
                    .addNumericField("CalculationBase", 3) // Calculation Base (e.g., 000)
                    // ------------------------------------------------------------------
                    // 3. Trailer Record ("FT")
                    // ------------------------------------------------------------------
                    .addRecord("FT", "File Trailer") // FT
                    .addAlphaNumericField("recordType", 2) // FT
                    .addNumericField("NoOfRecords", 12) // No Of records (e.g.,    144)
                    .build();

            FixedLengthReader reader = new StandardFixedLengthReader(fileSchema);

            // String rawLine = "FH00000007FXCURRENCY2026041000010001001376000";
            String rawLine = "RD20260410840                           3.215                000";
            //String rawLine = "FT         144";
            int lineNumber = 1;

            RecordData record = reader.readLine(rawLine, lineNumber);

            log.info("Record Type: {}", record.getRecordTypeCode()); // Output: DR
            log.info("Account:     {}", record.getValue("Currency")); // Output: 1234567
            log.info("Amount:      {}", record.getValue("EffectiveDate"));   // Output: 5000

            Iso8583Parser iso8583Parser = new Iso8583Parser();
            return iso8583Parser.parse(item).toString();
        } catch (IsoException e) {
            log.error("Error occurred while processing ISO 8583 message: {}", e.getMessage(), e);
            return null;
        }
    }
}
