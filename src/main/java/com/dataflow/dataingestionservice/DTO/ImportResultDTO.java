package com.dataflow.dataingestionservice.DTO;

public record ImportResultDTO(
        String message,
        int importedRows,
        int failedRows,
        String errorReportFileName,
        String errorReportBase64
) {
}
