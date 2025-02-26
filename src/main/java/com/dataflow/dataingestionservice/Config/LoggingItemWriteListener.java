package com.dataflow.dataingestionservice.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;

public class LoggingItemWriteListener<T> implements ItemWriteListener<T> {
    private static final Logger logger = LoggerFactory.getLogger(LoggingItemWriteListener.class);


    @Override
    public void beforeWrite(Chunk<? extends T> items) {
        for (T item : items) {
            logger.info("About to insert: {}", item);
        }
    }

    @Override
    public void afterWrite(Chunk<? extends T> items) {
        ItemWriteListener.super.afterWrite(items);
    }

    @Override
    public void onWriteError(Exception exception, Chunk<? extends T> items) {
        logger.error("Error writing items: {}", items, exception);
    }
}
