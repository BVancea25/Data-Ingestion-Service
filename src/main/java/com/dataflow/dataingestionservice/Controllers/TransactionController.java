package com.dataflow.dataingestionservice.Controllers;

import org.apache.commons.io.FilenameUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.Objects;

/**
 * REST controller for handling transaction file uploads.
 * <p>
 * This controller provides an endpoint for uploading transaction files in either CSV or Excel format.
 * Upon receiving a file, it creates a temporary file, constructs job parameters, and launches a Spring Batch
 * job to process the file. An optional date-time format parameter can be provided to influence the parsing
 * of date-time fields during processing.
 * </p>
 */
@RestController
@RequestMapping("/api")
public class TransactionController {

    private final JobLauncher jobLauncher;
    private final Job job;

    /**
     * Constructs a new TransactionController with the specified job launcher and job.
     *
     * @param jobLauncher the {@link JobLauncher} used to launch batch jobs
     * @param job         the {@link Job} that processes the transaction file
     */
    public TransactionController(JobLauncher jobLauncher, Job job) {
        this.jobLauncher = jobLauncher;
        this.job = job;
    }

    /**
     * Uploads a transaction file and launches a batch job to process it.
     * <p>
     * The endpoint expects a multipart file parameter named "file" and an optional "formatDateTime" parameter
     * that specifies the date-time format to be used during processing. The file's extension is determined to
     * create a temporary file with the appropriate extension. The temporary file's absolute path and the date-time
     * format are then passed as job parameters to the batch job.
     * </p>
     *
     * @param file            the uploaded file containing transaction data
     * @param formatDateTime  an optional date-time format string for parsing date fields (e.g., "ISO", "YYYY-MM-DD HH:mm:ss")
     * @return a {@link ResponseEntity} containing a success message if the job was started, or an error message if the file processing failed
     */
    @PostMapping("/transaction/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "formatDateTime", required = false) String formatDateTime) {
        try {

            String extension = FilenameUtils.getExtension(file.getOriginalFilename());

            File tempFile = File.createTempFile("upload-", "." + extension);

            file.transferTo(tempFile);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", tempFile.getAbsolutePath())
                    .addString("formatDateTime", Objects.requireNonNullElse(formatDateTime, ""))
                    .toJobParameters();

            // Launch the Spring Batch job
            jobLauncher.run(job, jobParameters);

            return ResponseEntity.ok("File uploaded and job started.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File processing failed.");
        }
    }
}
