package com.dataflow.dataingestionservice.Config;

import com.dataflow.dataingestionservice.Config.ItemProcessor.TransactionProcessor;
import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Repositories.CurrencyRepository;
import com.dataflow.dataingestionservice.Utils.ColumnFormatter;
import com.dataflow.dataingestionservice.Utils.LoggingItemWriteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.extensions.excel.streaming.StreamingXlsxItemReader;
import org.springframework.batch.extensions.excel.support.rowset.DefaultRowSetFactory;
import org.springframework.batch.extensions.excel.support.rowset.StaticColumnNameExtractor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.oxm.xstream.XStreamMarshaller;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Configuration class for setting up the Spring Batch job that processes transaction files.
 * <p>
 * This configuration supports both CSV and Excel (XLS/XLSX) file formats by creating the appropriate
 * {@code ItemReader} for each type. It also configures the {@code ItemProcessor}, {@code JdbcBatchItemWriter},
 * and the job/step definitions.
 * </p>
 */
@Configuration
public class TransactionBatchConfig {

    private static final Logger logger = LoggerFactory.getLogger(TransactionBatchConfig.class);

    @Autowired
    private CurrencyRepository currencyRepository;

    /**
     * Creates a {@link FlatFileItemReader} for reading transactions from a CSV file.
     *
     * @param filePath       the path to the CSV file
     * @param formatDateTime the date-time format to be used for parsing date fields.<b>Format is not required but will speed up the job</b>
     * @return a configured {@link FlatFileItemReader} for {@link Transaction} objects
     */
    public FlatFileItemReader<Transaction> csvItemReader(String filePath, String formatDateTime) {
        FlatFileItemReader<Transaction> reader = new FlatFileItemReader<>();
        reader.setSaveState(false);
        reader.setResource(new FileSystemResource(filePath));
        reader.setLinesToSkip(1);
        reader.setLineMapper(lineMapper(formatDateTime));
        return reader;
    }

    public StaxEventItemReader<Transaction> xmlItemReader(String filePath, String formatDateTime){
        StaxEventItemReader<Transaction> itemReader = new StaxEventItemReader<>();
        itemReader.setResource(new FileSystemResource(filePath));
        itemReader.setFragmentRootElementName("transaction");
        itemReader.setUnmarshaller(transactionMarshaller());

        return itemReader;
    }

    public XStreamMarshaller transactionMarshaller(){
        Map<String,Class> aliases = new HashMap<>();
        aliases.put("transaction", Transaction.class);
        aliases.put("amount", BigDecimal.class);
        aliases.put("currency", String.class);
        aliases.put("transaction_date", LocalDateTime.class);
        aliases.put("category", String.class);
        aliases.put("description", String.class);
        aliases.put("payment_mode", String.class);

        XStreamMarshaller marshaller = new XStreamMarshaller();

        marshaller.setAliases(aliases);

        return marshaller;
    }

    /**
     * Creates a {@link StreamingXlsxItemReader} for reading transactions from an Excel file.
     *
     * @param filePath       the path to the Excel file
     * @param formatDateTime the date-time format to be used for parsing date fields.<b>Format is not required but will speed up the job</b>
     * @return a configured {@link StreamingXlsxItemReader} for {@link Transaction} objects
     */
    public StreamingXlsxItemReader<Transaction> excelItemReader(String filePath, String formatDateTime) {
        StreamingXlsxItemReader<Transaction> poiItemReader = new StreamingXlsxItemReader<>();

        // Define the column names expected in the Excel file
        String[] columns = new String[] { "transactionDate", "category", "description", "amount", "currencyCode", "paymentMode"};
        StaticColumnNameExtractor columnNameExtractor = new StaticColumnNameExtractor(columns);
        DefaultRowSetFactory rowSetFactory = new DefaultRowSetFactory();
        rowSetFactory.setColumnNameExtractor(columnNameExtractor);

        poiItemReader.setLinesToSkip(1);
        poiItemReader.setRowMapper(rowMapper(formatDateTime));
        poiItemReader.setSaveState(false);
        poiItemReader.setResource(new FileSystemResource(filePath));
        poiItemReader.setRowSetFactory(rowSetFactory);

        return poiItemReader;
    }

    /**
     * Creates an {@link ExcelTransactionRowMapper} for mapping rows from an Excel file to {@link Transaction} objects.
     *
     * @param formatDateTime the date-time format to be used for parsing date fields
     * @return a configured {@link ExcelTransactionRowMapper}
     */
    private ExcelTransactionRowMapper rowMapper(String formatDateTime) {
        ExcelTransactionRowMapper rowMapper = new ExcelTransactionRowMapper(formatDateTime);
        rowMapper.setTargetType(Transaction.class);
        rowMapper.setStrict(false);
        return rowMapper;
    }

    /**
     * Creates a thread-safe {@link SynchronizedItemStreamReader} that delegates to either a CSV or Excel reader based on the file extension.
     * <p>
     * The appropriate reader is chosen based on the file extension provided in the job parameters.
     * </p>
     *
     * @param filePath       the path to the input file (CSV or Excel)
     * @param formatDateTime the date-time format to be used for parsing date fields
     * @return a configured {@link SynchronizedItemStreamReader} for {@link Transaction} objects
     */
    @Bean
    @StepScope
    public SynchronizedItemStreamReader<Transaction> transactionItemReader(
            @Value("#{jobParameters['filePath']}") String filePath,
            @Value("#{jobParameters['formatDateTime']}") String formatDateTime) {

        SynchronizedItemStreamReader<Transaction> syncReader = new SynchronizedItemStreamReader<>();
        if (filePath.toLowerCase().endsWith(".xlsx") || filePath.toLowerCase().endsWith(".xls")) {
            StreamingXlsxItemReader<Transaction> excelReader = excelItemReader(filePath, formatDateTime);
            syncReader.setDelegate(excelReader);
        } else if(filePath.toLowerCase().endsWith(".xml")) {
            StaxEventItemReader<Transaction> xmlReader = xmlItemReader(filePath, formatDateTime);
            syncReader.setDelegate(xmlReader);
        } else{
            FlatFileItemReader<Transaction> csvReader = csvItemReader(filePath, formatDateTime);
            syncReader.setDelegate(csvReader);
        }
        return syncReader;
    }

    /**
     * Creates a {@link LineMapper} for parsing CSV file lines into {@link Transaction} objects.
     *
     * @param formatDateTime the date-time format to be used for parsing date fields
     * @return a configured {@link LineMapper} for {@link Transaction} objects
     */
    private LineMapper<Transaction> lineMapper(String formatDateTime) {
        DefaultLineMapper<Transaction> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

        tokenizer.setDelimiter(",");
        tokenizer.setNames("transactionDate", "category", "description", "amount", "currencyCode", "paymentMode");
        tokenizer.setStrict(false);

        BeanWrapperFieldSetMapper<Transaction> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(Transaction.class);

        // Register custom editors for LocalDateTime and UUID conversion
        Map<Class<?>, PropertyEditor> customEditors = new HashMap<>();
        customEditors.put(LocalDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(ColumnFormatter.convertToLocalDateTime(text, formatDateTime));
            }
        });

        mapper.setCustomEditors(customEditors);

        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);

        return lineMapper;
    }

    /**
     * Creates an {@link ItemProcessor} to process {@link Transaction} objects.
     *
     * @return an {@link ItemProcessor} that processes transactions
     */
    @Bean
    public ItemProcessor<Transaction, Transaction> transactionProcessor() {
        return new TransactionProcessor(currencyRepository);
    }

    /**
     * Defines the Spring Batch job for inserting transactions.
     *
     * @param jobRepository        the {@link JobRepository} to use
     * @param insertStep           the {@link Step} to execute
     * @param jobExecutionListener a listener for job execution events
     * @return a configured {@link Job} for inserting transactions
     */
    @Bean
    @Qualifier("transactionJob")
    public Job insertJob(JobRepository jobRepository, Step insertStep, JobExecutionListener jobExecutionListener) {
        logger.info("🚀 insertJob() is being initialized...");
        return new JobBuilder("insertJob", jobRepository)
                .start(insertStep)
                .listener(jobExecutionListener)
                .build();
    }

    /**
     * Creates a {@link JdbcBatchItemWriter} for writing {@link Transaction} objects to the database.
     *
     * @param dataSource the {@link DataSource} for the database connection
     * @return a configured {@link JdbcBatchItemWriter} for {@link Transaction} objects
     */
    @Bean
    public JdbcBatchItemWriter<Transaction> jdbcBatchItemWriter(DataSource dataSource) {
        JdbcBatchItemWriter<Transaction> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql(
                "INSERT INTO transactions (id, user_id, transaction_date, category, description, amount, currency_id, payment_mode, created_at) " +
                        "VALUES (:id,:userId, :transactionDate, :category, :description, :amount, :currencyId, :paymentMode, :createdAt) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "category = VALUES(category), " +
                        "description = VALUES(description), " +
                        "amount = VALUES(amount), " +
                        "currency_id = VALUES(currency_id), " +
                        "payment_mode = VALUES(payment_mode), " +
                        "created_at = VALUES(created_at)");
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Transaction>(){
            @Override
            public SqlParameterSource createSqlParameterSource(Transaction item) {
                MapSqlParameterSource paramSource = new MapSqlParameterSource();
                paramSource.addValue("id", item.getIdAsString());
                paramSource.addValue("userId", item.getUserIdAsString());
                paramSource.addValue("transactionDate", item.getTransactionDate());
                paramSource.addValue("category", item.getCategory());
                paramSource.addValue("description", item.getDescription());
                paramSource.addValue("amount", item.getAmount());
                paramSource.addValue("currencyId", item.getCurrency().getIdAsString());
                paramSource.addValue("paymentMode", item.getPaymentMode());
                paramSource.addValue("createdAt", item.getCreatedAt());
                return paramSource;
            }
        });

        return writer;
    }

    /**
     * Defines the step that processes transactions.
     * <p>
     * This step reads transactions using the {@code transactionItemReader}, processes them via the configured
     * {@code ItemProcessor}, and writes them to the database using the {@code JdbcBatchItemWriter}.
     * </p>
     *
     * @param jobRepository     the {@link JobRepository} for the job
     * @param transactionManager the {@link PlatformTransactionManager} for managing transactions
     * @param itemReader        the thread-safe reader for {@link Transaction} objects
     * @param transactionProcessor     the processor for {@link Transaction} objects
     * @param itemWriter        the writer for {@link Transaction} objects
     * @return a configured {@link Step} for processing transactions
     */
    @Bean
    public Step insertStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           SynchronizedItemStreamReader<Transaction> itemReader,
                           ItemProcessor<Transaction, Transaction> transactionProcessor,
                           JdbcBatchItemWriter<Transaction> itemWriter) {

        return new StepBuilder("insertStep", jobRepository)
                .<Transaction, Transaction>chunk(50, transactionManager)
                .reader(itemReader)
                .processor(transactionProcessor)
                .writer(itemWriter)
                .listener(new LoggingItemWriteListener<>())
                .build();
    }

    /**
     * Creates a {@link JobExecutionListener} that performs post-job cleanup.
     * <p>
     * This listener deletes the input file after the job execution is complete.
     * </p>
     *
     * @param filePath the path to the input file (injected from job parameters)
     * @return a {@link JobExecutionListener} for cleanup after job execution
     */
    @Bean
    @JobScope
    public JobExecutionListener jobExecutionListener(@Value("#{jobParameters['filePath']}") String filePath) {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                // No action required before the job starts.
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                File file = new File(filePath);
                if (file.delete()) {
                    logger.info("File from " + filePath + " deleted.");
                } else {
                    logger.error("Failed to delete " + filePath);
                }
            }
        };
    }
}
