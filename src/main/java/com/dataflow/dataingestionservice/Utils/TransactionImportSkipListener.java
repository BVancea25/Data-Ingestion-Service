package com.dataflow.dataingestionservice.Utils;

import com.dataflow.dataingestionservice.DTO.ImportErrorRowDTO;
import com.dataflow.dataingestionservice.Models.Transaction;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@StepScope
public class TransactionImportSkipListener implements SkipListener<Transaction, Transaction>, StepExecutionListener {

    public static final String IMPORT_ERRORS_CONTEXT_KEY = "transactionImportErrors";

    private final List<ImportErrorRowDTO> errors = new ArrayList<>();

    @Override
    public void beforeStep(StepExecution stepExecution) {
        errors.clear();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        stepExecution.getExecutionContext().put(IMPORT_ERRORS_CONTEXT_KEY, new ArrayList<>(errors));
        return stepExecution.getExitStatus();
    }

    @Override
    public void onSkipInRead(Throwable t) {
        Integer rowNumber = null;
        String rawData = "";

        if (t instanceof FlatFileParseException ex) {
            rowNumber = ex.getLineNumber();
            rawData = ex.getInput();
        }

        addError(new ImportErrorRowDTO(rowNumber, "READ", cleanMessage(t), rawData));
    }

    @Override
    public void onSkipInProcess(Transaction item, Throwable t) {
        addError(new ImportErrorRowDTO(null, "PROCESS", cleanMessage(t), describe(item)));
    }

    @Override
    public void onSkipInWrite(Transaction item, Throwable t) {
        addError(new ImportErrorRowDTO(null, "WRITE", cleanMessage(t), describe(item)));
    }

    @SuppressWarnings("unchecked")
    private void addError(ImportErrorRowDTO error) {
        errors.add(error);

        var context = StepSynchronizationManager.getContext();
        if (context == null) {
            return;
        }

        var executionContext = context.getStepExecution().getExecutionContext();
        Object current = executionContext.get(IMPORT_ERRORS_CONTEXT_KEY);
        List<ImportErrorRowDTO> contextErrors = current instanceof List<?>
                ? (List<ImportErrorRowDTO>) current
                : new ArrayList<>();

        contextErrors.add(error);
        executionContext.put(IMPORT_ERRORS_CONTEXT_KEY, new ArrayList<>(contextErrors));
    }

    private String cleanMessage(Throwable t) {
        if (t == null) {
            return "Unknown import error";
        }

        String message = t.getMessage();
        return message == null || message.isBlank() ? t.getClass().getSimpleName() : message;
    }

    private String describe(Transaction item) {
        if (item == null) {
            return "";
        }

        return "transactionDate=" + item.getTransactionDate()
                + ", categoryName=" + item.getCategoryName()
                + ", description=" + item.getDescription()
                + ", amount=" + item.getAmount()
                + ", currencyCode=" + item.getCurrencyCode()
                + ", paymentMode=" + item.getPaymentMode();
    }
}
