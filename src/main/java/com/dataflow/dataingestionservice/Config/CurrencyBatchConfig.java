package com.dataflow.dataingestionservice.Config;

import com.dataflow.dataingestionservice.Config.ItemProcessor.CurrencyProcessor;
import com.dataflow.dataingestionservice.Models.Currency;
import com.dataflow.dataingestionservice.Utils.ColumnFormatter;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class CurrencyBatchConfig {
    private static final Logger logger = LoggerFactory.getLogger(TransactionBatchConfig.class);

    @Bean
    @StepScope
    @Qualifier("currencyReader")
    public FlatFileItemReader<Currency> csvItemReader( @Value("#{jobParameters['filePath']}") String filePath) {
        FlatFileItemReader<Currency> reader = new FlatFileItemReader<>();
        reader.setSaveState(false);
        reader.setResource(new FileSystemResource(filePath));
        reader.setLinesToSkip(1);
        reader.setLineMapper(lineMapper());
        return reader;
    }

    private LineMapper<Currency> lineMapper() {
        DefaultLineMapper<Currency> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

        tokenizer.setDelimiter(",");
        tokenizer.setNames("code", "name");
        tokenizer.setStrict(false);


        BeanWrapperFieldSetMapper<Currency> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(Currency.class);
        Map<Class<?>, PropertyEditor> customEditors = new HashMap<>();

        //mapper.setCustomEditors(customEditors);

        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);

        return lineMapper;
    }

    @Bean
    public JpaItemWriter<Currency> currencyJpaItemWriter(EntityManagerFactory entityManagerFactory){
        JpaItemWriter<Currency> itemWriter = new JpaItemWriter<>();
        itemWriter.setEntityManagerFactory(entityManagerFactory);
        return itemWriter;
    }

    @Bean
    public ItemProcessor<Currency, Currency> ccurrencyItemProcessor(){
        return new CurrencyProcessor();
    }
    @Bean
    @Qualifier("currencyInsertStep")
    public Step currencyInsertStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           @Qualifier("currencyReader") FlatFileItemReader<Currency> fileItemReader,
                           ItemProcessor<Currency,Currency> currencyItemProcessor,
                           JpaItemWriter<Currency> currencyJpaItemWriter) {

        return new StepBuilder("insertCurrencyStep", jobRepository)
                .<Currency, Currency>chunk(50, transactionManager)
                .reader(fileItemReader)
                .processor(currencyItemProcessor)
                .writer(currencyJpaItemWriter)
                .build();
    }
    @Bean
    @Qualifier("currencyJob")
    public Job currencyInsertJob(JobRepository jobRepository,@Qualifier("currencyInsertStep") Step currencyInsertStep, JobExecutionListener jobExecutionListener) {
        logger.info("🚀 currencyInsertJob() is being initialized...");
        return new JobBuilder("currencyInsertJob", jobRepository)
                .start(currencyInsertStep)
                .listener(jobExecutionListener)
                .build();
    }
}
