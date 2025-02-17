package com.dataflow.dataingestionservice.Config;

import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Repositories.TransactionRepository;
import com.dataflow.dataingestionservice.Utils.ColumnFormatter;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


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
    public FlatFileItemReader<Transaction> itemReader(
            @Value("#{jobParameters['filePath']}") String filePath,
            @Value("#{jobParameters['formatDateTime']}") String formatDateTime
            ) {
        FlatFileItemReader<Transaction> reader = new FlatFileItemReader<>();
        reader.setSaveState(false);
        reader.setResource(new FileSystemResource(filePath));
        reader.setLinesToSkip(1);
        reader.setLineMapper(lineMapper(formatDateTime));
        return reader;
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<Transaction> synchronizedReader(
            FlatFileItemReader<Transaction> delegateReader) {
        SynchronizedItemStreamReader<Transaction> syncReader = new SynchronizedItemStreamReader<>();
        syncReader.setDelegate(delegateReader);
        return syncReader;
    }


    private LineMapper<Transaction> lineMapper(String formatDateTime){
        DefaultLineMapper<Transaction> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

        tokenizer.setDelimiter(",");
        tokenizer.setNames("userId","transactionDate","category","description","amount","currency","paymentMode");
        tokenizer.setStrict(false);

        BeanWrapperFieldSetMapper<Transaction> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(Transaction.class);

        Map<Class<?>, PropertyEditor> customEditors = new HashMap<>();
        customEditors.put(LocalDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(ColumnFormatter.convertToLocalDateTime(text,formatDateTime));
            }
        });
        customEditors.put(UUID.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(ColumnFormatter.convertStringToUUID(text));
            }
        });
        mapper.setCustomEditors(customEditors);

        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);

        return lineMapper;
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> processor(){
        return new TransactionProcessor();
    }

    @Bean
    public Job insertJob(JobRepository jobRepository, Step insertStep, JobExecutionListener jobExecutionListener){
        logger.info("🚀 insertJob() is being initialized...");
       return new JobBuilder("insertJob",jobRepository)
               .start(insertStep)
               .listener(jobExecutionListener)
               .build();
    }

    @Bean
    public JpaItemWriter<Transaction> jpaItemWriter(EntityManagerFactory entityManagerFactory){
        JpaItemWriter<Transaction> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        writer.setUsePersist(false);
        return writer;
    }

    @Bean
    public Step insertStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           SynchronizedItemStreamReader<Transaction> itemReader,
                           ItemProcessor<Transaction, Transaction> itemProcessor,
                           JpaItemWriter<Transaction> itemWriter,
                           @Qualifier("taskExecutorInsertJob") TaskExecutor taskExecutor) {

        return new StepBuilder("insertStep", jobRepository)
                .<Transaction, Transaction>chunk(50, transactionManager)
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @Qualifier("taskExecutorInsertJob")
    public TaskExecutor taskExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("batch-thread-");
        executor.initialize();
        return executor;
    }

    @Bean
    @JobScope
    public JobExecutionListener jobExecutionListener(@Value("#{jobParameters['filePath']}") String filePath){

        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                JobExecutionListener.super.beforeJob(jobExecution);
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                BatchStatus status=jobExecution.getStatus();
                if(!status.isUnsuccessful()){
                    File file = new File(filePath);
                    if(file.delete()){
                        logger.info("File from "+filePath+" delete.");
                    }else {
                        logger.error("Failed to delete "+filePath);
                    }
                }

            }
        };
    }



}
