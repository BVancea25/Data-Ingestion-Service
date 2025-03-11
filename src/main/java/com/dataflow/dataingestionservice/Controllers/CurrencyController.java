package com.dataflow.dataingestionservice.Controllers;


import com.dataflow.dataingestionservice.Config.TransactionBatchConfig;
import com.dataflow.dataingestionservice.Models.Currency;
import com.dataflow.dataingestionservice.Services.CurrencyService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
public class CurrencyController {
    private static final List<String> ALLOWED_EXTENSIONS = List.of("csv");
    private static final Logger logger = LoggerFactory.getLogger(TransactionBatchConfig.class);
    private final JobLauncher jobLauncher;
    private final Job job;
    private final CurrencyService currencyService;

    public CurrencyController(JobLauncher jobLauncher, @Qualifier("currencyInsertJob") Job job, CurrencyService currencyService) {
        this.jobLauncher = jobLauncher;
        this.job = job;
        this.currencyService=currencyService;
    }
    @PostMapping("/currency/upload")
    public ResponseEntity<String> importCurrencies(@RequestParam("file") MultipartFile file){

        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is missing or empty.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file name.");
        }

        // Validate file extension
        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Unsupported file extension. Allowed extensions: csv");
        }

        try {
            // Create a temporary file with the proper extension
            File tempFile = File.createTempFile("upload-", "." + extension);
            file.transferTo(tempFile);

            // Build job parameters
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", tempFile.getAbsolutePath())
                    .toJobParameters();

            jobLauncher.run(job, jobParameters);

            return ResponseEntity.ok("File uploaded and job started.");
        } catch (Exception e) {
            logger.error("Error processing file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File processing failed: " + e.getMessage());
        }
    }

    @GetMapping("/currency")
    public List<Currency> getCurrencies(){
        return currencyService.getCurrencies();
    }

    @GetMapping("/currency/id/{id}")
    public Optional<Currency> getCurrencyWithId(@PathVariable("id") UUID uuid){
        return currencyService.getCurrencyById(uuid);
    }

    @GetMapping("/currency/code/{code}")
    public Currency getCurrencyWitchCode(@PathVariable("code") String code){
        return currencyService.getCurrencyByCode(code);
    }
}
