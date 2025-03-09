package com.dataflow.dataingestionservice.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;

/**
 * A custom {@link ItemWriteListener} implementation that logs details about items being written.
 * <p>
 * This listener logs each item before it is inserted into the database and logs any errors that occur during
 * the writing process.
 * </p>
 *
 * @param <T> the type of items to be written
 */
public class LoggingItemWriteListener<T> implements ItemWriteListener<T> {

    private static final Logger logger = LoggerFactory.getLogger(LoggingItemWriteListener.class);

    /**
     * Called before a chunk of items is written.
     * <p>
     * This implementation logs an info message for each item that is about to be inserted.
     * </p>
     *
     * @param items the chunk of items that is about to be written
     */
    @Override
    public void beforeWrite(Chunk<? extends T> items) {
        for (T item : items) {
            logger.info("About to insert: {}", item);
        }
    }

    /**
     * Called after a chunk of items has been successfully written.
     * <p>
     * This implementation delegates to the default behavior.
     * </p>
     *
     * @param items the chunk of items that has been written
     */
    @Override
    public void afterWrite(Chunk<? extends T> items) {
        ItemWriteListener.super.afterWrite(items);
    }

    /**
     * Called if an error occurs while writing a chunk of items.
     * <p>
     * This implementation logs an error message including the exception and the items that failed to be written.
     * </p>
     *
     * @param exception the exception that occurred during the write operation
     * @param items     the chunk of items that were being written when the error occurred
     */
    @Override
    public void onWriteError(Exception exception, Chunk<? extends T> items) {
        logger.error("Error writing items: {}", items, exception);
    }
}
