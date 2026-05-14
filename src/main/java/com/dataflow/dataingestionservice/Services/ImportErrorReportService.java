package com.dataflow.dataingestionservice.Services;

import com.dataflow.dataingestionservice.DTO.ImportErrorRowDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImportErrorReportService {

    public String toCsv(List<ImportErrorRowDTO> errors) {
        StringBuilder csv = new StringBuilder("rowNumber,phase,error,rawData\n");

        for (ImportErrorRowDTO error : errors) {
            csv.append(escape(error.rowNumber() == null ? "" : error.rowNumber().toString()))
                    .append(',')
                    .append(escape(error.phase()))
                    .append(',')
                    .append(escape(error.error()))
                    .append(',')
                    .append(escape(error.rawData()))
                    .append('\n');
        }

        return csv.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }

        return escaped;
    }
}
