package com.dataflow.dataingestionservice.Config;

import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Repositories.TransactionRepository;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.PlatformTransactionManager;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;


@Configuration
@EnableBatchProcessing
public class IngestBatchConfig {

   private final TransactionRepository transactionRepository;

    private static final Logger logger = LoggerFactory.getLogger(IngestBatchConfig.class);

    public IngestBatchConfig(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @PostConstruct
    public void checkRepositoryInjection() {
        if (transactionRepository == null) {
            logger.error("TransactionRepository is NOT injected!");
        } else {
            logger.info("TransactionRepository injected successfully: {}", transactionRepository.getClass().getName());
        }
    }


    @Bean
    @StepScope
    public FlatFileItemReader<Transaction> itemReader(@Value("#{jobParameters['filePath']}") String filePath) {
        FlatFileItemReader<Transaction> reader = new FlatFileItemReader<>();

        reader.setResource(new FileSystemResource(filePath));
        reader.setLinesToSkip(1);
        reader.setLineMapper(lineMapper());
        return reader;
    }

    private LineMapper<Transaction> lineMapper(){
        DefaultLineMapper<Transaction> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

        tokenizer.setDelimiter(",");
        tokenizer.setNames("userId","transactionDate","category","description","amount","currency","paymentMode"); //dupa pozitie nu dupa nume
        tokenizer.setStrict(false);

        BeanWrapperFieldSetMapper<Transaction> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(Transaction.class);

        mapper.setCustomEditors(Collections.singletonMap(
                LocalDateTime.class, new PropertyEditorSupport() {
                    @Override
                    public void setAsText(String text) {
                        setValue(LocalDateTime.parse(text));
                    }
                })
        );

        mapper.setCustomEditors(Collections.singletonMap(
                BigDecimal.class, new PropertyEditorSupport() {
                    @Override
                    public void setAsText(String text) {
                        setValue(new BigDecimal(text));
                    }
                })
        );

        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);

        return lineMapper;
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> processor(){
        return new TransactionProcessor();
    }

    @Bean
    public Job insertJob(JobRepository jobRepository, Step insertStep){
        logger.info("🚀 insertJob() is being initialized...");
       return new JobBuilder("insertJob",jobRepository)
               .start(insertStep)
               .build();
    }

    @Bean
    public ItemWriter<Transaction> itemWriter() {
        logger.info("TransactionRepository injected successfully again: {}", transactionRepository.getClass().getName());

        RepositoryItemWriter<Transaction> itemWriter = new RepositoryItemWriter<>();
        itemWriter.setRepository(transactionRepository);
        itemWriter.setMethodName("save");
        return itemWriter;
    }

    @Bean
    public Step insertStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           FlatFileItemReader<Transaction> itemReader,
                           ItemProcessor<Transaction, Transaction> itemProcessor,
                           ItemWriter<Transaction> itemWriter) {

        return new StepBuilder("insertStep", jobRepository)
                .<Transaction, Transaction>chunk(50, transactionManager)
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .build();
    }



}
