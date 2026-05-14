package com.dataflow.dataingestionservice.DTO;

import java.io.Serializable;

public record ImportErrorRowDTO(
        Integer rowNumber,
        String phase,
        String error,
        String rawData
) implements Serializable {
}
