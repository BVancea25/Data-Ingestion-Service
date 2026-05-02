package com.dataflow.dataingestionservice.Controllers;

import com.dataflow.dataingestionservice.Config.TransactionBatchConfig;
import com.dataflow.dataingestionservice.DTO.ImportErrorRowDTO;
import com.dataflow.dataingestionservice.DTO.ImportResultDTO;
import com.dataflow.dataingestionservice.DTO.TransactionDTO;
import com.dataflow.dataingestionservice.DTO.TransactionFilter;
import com.dataflow.dataingestionservice.DTO.UpdateTransactionDTO;
import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Services.ImportErrorReportService;
import com.dataflow.dataingestionservice.Services.TransactionService;
import com.dataflow.dataingestionservice.Utils.Constants.PaymentMethod;
import com.dataflow.dataingestionservice.Utils.Constants.TransactionType;
import com.dataflow.dataingestionservice.Utils.SecurityUtils;
import com.dataflow.dataingestionservice.Utils.TransactionImportSkipListener;
import org.apache.commons.io.FilenameUtils;
import org.springframework.batch.core.BatchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * REST controller for handling transaction file uploads and posting transactions.
 * <p>
 * This controller provides endpoints to:
 * <ul>
 *     <li>Upload a transaction file (CSV/Excel), which creates a temporary file and launches a Spring Batch job.</li>
 *     <li>Submit a list of {@link Transaction} objects for direct persistence.</li>
 * </ul>
 * Input is validated and sanitized; descriptive responses are returned when errors occur.
 * </p>
 */
@RestController
@RequestMapping("/api")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionBatchConfig.class);

    // List of allowed file extensions.
    private static final List<String> ALLOWED_EXTENSIONS = List.of("csv", "xlsx", "xls", "xml");

    private final TransactionService transactionService;
    private final ImportErrorReportService importErrorReportService;
    private final JobLauncher jobLauncher;
    private final Job job;

    /**
     * Constructs a new TransactionController.
     *
     * @param jobLauncher        the {@link JobLauncher} used to launch batch jobs
     * @param job                the {@link Job} that processes the transaction file
     * @param transactionService the service used to save transactions directly
     */
    public TransactionController(JobLauncher jobLauncher,
                                 @Qualifier("transactionJob") Job job,
                                 TransactionService transactionService,
                                 ImportErrorReportService importErrorReportService) {
        this.jobLauncher = jobLauncher;
        this.job = job;
        this.transactionService = transactionService;
        this.importErrorReportService = importErrorReportService;
    }

    /**
     * Uploads a transaction file and launches a batch job to process it.
     * <p>
     * The endpoint expects a multipart file parameter named "file" and an optional "formatDateTime"
     * parameter (e.g., "ISO", "YYYY-MM-DD HH:mm:ss") for parsing date fields.
     * The file is validated for presence, non-emptiness, and acceptable extension.
     * </p>
     *
     * @param file           the uploaded file containing transaction data
     * @param formatDateTime an optional date-time format string
     * @return a {@link ResponseEntity} with a success message or an error message describing the issue
     */
    @PostMapping("/income/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "formatDateTime", required = false) String formatDateTime) {
        // Validate that the file is present and not empty
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is missing or empty.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file name.");
        }

        // Validate file extension
        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Unsupported file extension. Allowed extensions: csv, xlsx, xls, xml.");
        }

        try {
            // Create a temporary file with the proper extension
            File tempFile = File.createTempFile("upload-", "." + extension);
            file.transferTo(tempFile);

            // Build job parameters
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", tempFile.getAbsolutePath())
                    .addString("formatDateTime", Objects.requireNonNullElse(formatDateTime, ""))
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            if (jobExecution.getStatus() == BatchStatus.FAILED) {
                String failureMessage = jobExecution.getAllFailureExceptions()
                        .stream()
                        .map(Throwable::getMessage)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("Batch job failed.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("File processing failed: " + failureMessage);
            }

            int importedRows = Math.toIntExact(jobExecution.getStepExecutions()
                    .stream()
                    .mapToLong(StepExecution::getWriteCount)
                    .sum());

            List<ImportErrorRowDTO> errors = getImportErrors(jobExecution);
            int skippedRows = Math.toIntExact(jobExecution.getStepExecutions()
                    .stream()
                    .mapToLong(step -> step.getReadSkipCount() + step.getProcessSkipCount() + step.getWriteSkipCount())
                    .sum());

            if (errors.isEmpty() && skippedRows > 0) {
                errors.add(new ImportErrorRowDTO(
                        null,
                        "IMPORT",
                        "Some rows were skipped during import. Check the service logs for detailed row-level errors.",
                        ""
                ));
            }

            int failedRows = skippedRows > 0 ? skippedRows : errors.size();

            String errorReportFileName = null;
            String errorReportBase64 = null;

            if (failedRows > 0) {
                String report = importErrorReportService.toCsv(errors);
                errorReportFileName = "transaction-import-errors.csv";
                errorReportBase64 = Base64.getEncoder()
                        .encodeToString(report.getBytes(StandardCharsets.UTF_8));
            }

            String message = failedRows == 0
                    ? "File imported successfully."
                    : "File imported with skipped rows.";

            return ResponseEntity.ok(new ImportResultDTO(
                    message,
                    importedRows,
                    failedRows,
                    errorReportFileName,
                    errorReportBase64
            ));
        } catch (Exception e) {
            logger.error("Error processing file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File processing failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<ImportErrorRowDTO> getImportErrors(JobExecution jobExecution) {
        List<ImportErrorRowDTO> errors = new ArrayList<>();

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            Object value = stepExecution.getExecutionContext()
                    .get(TransactionImportSkipListener.IMPORT_ERRORS_CONTEXT_KEY);

            if (value instanceof List<?>) {
                errors.addAll((List<ImportErrorRowDTO>) value);
            }
        }

        return errors;
    }

    /**
     * Accepts a list of {@link Transaction} objects and saves them.
     * <p>
     * The endpoint validates that the list is not null or empty.
     * </p>
     *
     * @param transactions the list of transactions to save
     * @return a {@link ResponseEntity} indicating success or failure
     */
    @PostMapping("/income")
    public ResponseEntity<String> postTransaction(@RequestBody List<TransactionDTO> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Transaction list is empty.");
        }
        try {
            transactionService.saveTransactions(transactions);
            return ResponseEntity.ok("Transactions saved successfully.");
        } catch (Exception e) {
            logger.error("Error saving transactions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Saving transactions failed: " + e.getMessage());
        }
    }

    @GetMapping("/incomes")
    public Page<TransactionDTO> getCurrentUserTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String currencyCode,
            @RequestParam(required = false) PaymentMethod paymentMode,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type

            ){
        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page,size,sort);
        System.out.println(paymentMode);
        TransactionFilter filter = new TransactionFilter();
        filter.setCategoryId(categoryId);
        filter.setCurrencyCode(currencyCode);
        filter.setPaymentMode(paymentMode);
        filter.setDescription(description);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setType(type);

        String userId = SecurityUtils.getCurrentUserUuid();
        return transactionService.getCurrentUserTransactions(pageable,filter,userId);
    }

    @DeleteMapping("/incomes/delete")
    public ResponseEntity<Void> deleteTransactions(@RequestBody List<String> ids){
        transactionService.deleteTransactions(ids, SecurityUtils.getCurrentUserUuid());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/income")
    public ResponseEntity<String> updateTransaction(@RequestBody TransactionDTO transactionDTO){
        transactionService.updateTransaction(transactionDTO);
        return ResponseEntity.ok("Updated transaction");
    }

    @PutMapping("/incomes")
    public ResponseEntity<String> updateTransactions(@RequestBody UpdateTransactionDTO updateTransactionDTO){
        transactionService.updateTransactions(updateTransactionDTO);
        return ResponseEntity.ok("Updated transaction");
    }
}
