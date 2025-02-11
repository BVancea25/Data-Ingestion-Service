package com.dataflow.dataingestionservice.Controllers;

import com.dataflow.dataingestionservice.Config.IngestBatchConfig;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/api")
public class TransactionController {


    private final JobLauncher jobLauncher;
    private final Job job;

    public TransactionController(JobLauncher jobLauncher, Job job) {
        this.jobLauncher = jobLauncher;
        this.job = job;
    }


    @PostMapping("/transaction/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Create a temporary file; you can also specify a directory if needed
            File tempFile = File.createTempFile("upload-", ".csv");
            // Save the uploaded file to disk
            file.transferTo(tempFile);

            // Build job parameters; adding a timestamp ensures uniqueness for each job execution
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", tempFile.getAbsolutePath())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // Launch the job with the job parameters
            JobExecution execution = jobLauncher.run(job, jobParameters);

            return ResponseEntity.ok("File uploaded and job started.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File processing failed.");
        }
    }
}
