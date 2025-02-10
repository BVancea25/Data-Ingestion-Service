package com.dataflow.dataingestionservice.Config;

import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Repositories.TransactionRepository;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableBatchProcessing(transactionManagerRef = "jpaTransactionManager")
public class IngestBatchConfig {

   private final TransactionRepository transactionRepository;

   public IngestBatchConfig(TransactionRepository transactionRepository){
       this.transactionRepository=transactionRepository;
   }

    @Bean
    public FlatFileItemReader<Transaction> itemReader() {
        FlatFileItemReader<Transaction> reader = new FlatFileItemReader<>();
        reader.setLinesToSkip(1); // Skip CSV header
        reader.setLineMapper(lineMapper());
        return reader;
    }

    private LineMapper<Transaction> lineMapper(){
        DefaultLineMapper<Transaction> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

        tokenizer.setDelimiter(",");
        tokenizer.setNames("user_id","transaction_date","category","description","amount","currency","payment_mode"); //dupa pozitie nu dupa nume
        tokenizer.setStrict(false);

        BeanWrapperFieldSetMapper<Transaction> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(Transaction.class);

        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);

        return lineMapper;
    }

    @Bean
    public TransactionProcessor processor(){
        return new TransactionProcessor();
    }



   @Bean
    public RepositoryItemWriter itemWriter(){
        RepositoryItemWriter<Transaction> itemWriter = new RepositoryItemWriter<>();
        itemWriter.setRepository(transactionRepository);
        itemWriter.setMethodName("save");
        return itemWriter;
   }


}
