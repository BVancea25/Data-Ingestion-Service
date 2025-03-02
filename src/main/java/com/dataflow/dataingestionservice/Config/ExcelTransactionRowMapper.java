package com.dataflow.dataingestionservice.Config;

import com.dataflow.dataingestionservice.Models.Transaction;
import com.dataflow.dataingestionservice.Utils.ColumnFormatter;
import org.springframework.batch.extensions.excel.mapping.BeanWrapperRowMapper;
import org.springframework.validation.DataBinder;
import java.beans.PropertyEditorSupport;
import java.time.LocalDateTime;

/**
 * A custom {@link BeanWrapperRowMapper} implementation for mapping rows from an Excel file to {@link Transaction} objects.
 * <p>
 * This row mapper registers a custom property editor for {@link LocalDateTime} that converts date-time strings
 * using the specified format. It leverages the {@link ColumnFormatter#convertToLocalDateTime(String, String)} method
 * to perform the conversion.
 * </p>
 */
public class ExcelTransactionRowMapper extends BeanWrapperRowMapper<Transaction> {

    /**
     * The date-time format to be used for parsing date strings.
     */
    private final String formatDateTime;

    /**
     * Constructs a new {@code ExcelTransactionRowMapper} with the given date-time format.
     *
     * @param formatDateTime the date-time format to be used for converting String values to {@link LocalDateTime}
     */
    public ExcelTransactionRowMapper(String formatDateTime) {
        this.formatDateTime = formatDateTime;
    }

    /**
     * Initializes the data binder with a custom property editor for {@link LocalDateTime}.
     * <p>
     * This method registers a custom editor that converts a String representation of a date-time to a {@link LocalDateTime}
     * using the format provided at construction.
     * </p>
     *
     * @param binder the {@link DataBinder} to initialize
     */
    @Override
    protected void initBinder(DataBinder binder) {
        super.initBinder(binder);

        binder.registerCustomEditor(LocalDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(ColumnFormatter.convertToLocalDateTime(text, formatDateTime));
            }
        });
    }
}
