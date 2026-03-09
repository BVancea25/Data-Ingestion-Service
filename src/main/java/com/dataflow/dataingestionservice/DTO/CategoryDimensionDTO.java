package com.dataflow.dataingestionservice.DTO;

import com.dataflow.dataingestionservice.Utils.Constants.SyncOperation;
import com.dataflow.dataingestionservice.Utils.Constants.TransactionType;

public record CategoryDimensionDTO(String categoryId, String name, SyncOperation operation, TransactionType type, String userId) {
    @Override
    public String toString() {
        return "CategoryDimensionDTO{" +
                "categoryId='" + categoryId + '\'' +
                ", name='" + name + '\'' +
                ", operation=" + operation +
                ", type=" + type +
                ", userId='" + userId + '\'' +
                '}';
    }
}
