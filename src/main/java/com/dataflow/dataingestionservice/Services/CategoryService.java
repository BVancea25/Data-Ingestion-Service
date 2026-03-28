package com.dataflow.dataingestionservice.Services;

import com.dataflow.dataingestionservice.Config.ServiceProperties.ReportingServiceApiProperties;
import com.dataflow.dataingestionservice.DTO.CategoryDimensionDTO;
import com.dataflow.dataingestionservice.DTO.FactTransactionDTO;
import com.dataflow.dataingestionservice.Models.Category;
import com.dataflow.dataingestionservice.Repositories.CategoryRepository;
import com.dataflow.dataingestionservice.Utils.Constants.SyncOperation;
import com.dataflow.dataingestionservice.Utils.Constants.TransactionType;
import com.dataflow.dataingestionservice.Utils.SecurityUtils;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private final ReportingServiceApiProperties reportingServiceApiProperties;
    private final RestTemplate restTemplate;
    public CategoryService(CategoryRepository categoryRepository, ReportingServiceApiProperties reportingServiceApiProperties, RestTemplate restTemplate) {
        this.categoryRepository = categoryRepository;
        this.reportingServiceApiProperties = reportingServiceApiProperties;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Category create(Category category) {
        String userId = SecurityUtils.getCurrentUserUuid();
        String categoryId = UUID.randomUUID().toString();
        category.setUserId(userId);
        category.setId(categoryId);
        // Prevent duplicates per user + type
        boolean exists = categoryRepository.existsByUserIdAndName(
                userId,
                category.getName()
        );

        if (exists) {
            throw new IllegalArgumentException(
                    "Category already exists " + category.getName()
            );
        }

        Category saved =  categoryRepository.save(category);

        CategoryDimensionDTO dto = mapToCategoryDimensionDTO(category, SyncOperation.CREATE);
        sendToReportingService(Collections.singletonList(dto));

        return saved;
    }

    @Transactional
    public Category update(String categoryId, Category updated) {
        String userId = SecurityUtils.getCurrentUserUuid();

        Category existing = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!existing.getUserId().equals(userId)) {
            throw new SecurityException("Not allowed to update this category");
        }

        boolean exists = categoryRepository.existsByUserIdAndName(
                userId,
                updated.getName()
        );

        if (!exists) {
                throw new IllegalArgumentException(
                        "Category doesn't exists, can't update " + updated.getName()
                );
        }

        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setColor(updated.getColor());

        Category saved = categoryRepository.save(existing);

        CategoryDimensionDTO dto = mapToCategoryDimensionDTO(saved, SyncOperation.UPDATE);
        sendToReportingService(Collections.singletonList(dto));

        return saved;
    }


    public List<Category> getAll() {
        String userId = SecurityUtils.getCurrentUserUuid();
        return categoryRepository.findAllByUserId(userId);
    }

    public List<Category> getByType(TransactionType type) {
        String userId = SecurityUtils.getCurrentUserUuid();
        return categoryRepository.findAllByUserIdAndType(userId, type);
    }

    public Category getById(String categoryId) {
        String userId = SecurityUtils.getCurrentUserUuid();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getUserId().equals(userId)) {
            throw new SecurityException("Not allowed to access this category");
        }

        return category;
    }

    protected CategoryDimensionDTO mapToCategoryDimensionDTO(Category cat, SyncOperation operation){
        CategoryDimensionDTO dto = new CategoryDimensionDTO(
                cat.getId(),
                cat.getName(),
                operation,
                cat.getType(),
                cat.getUserId()
        );

        return dto;
    }

    public void sendToReportingService(List<CategoryDimensionDTO> dto) {
        System.out.println(reportingServiceApiProperties.getApiBase());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-REPORTING-API-KEY", reportingServiceApiProperties.getApiKey());

        HttpEntity<List<CategoryDimensionDTO>> request = new HttpEntity<>(dto,headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                reportingServiceApiProperties.getApiBase() + "/api/categories/sync",
                request,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(
                    "Failed to sync with Reporting Service: " + response.getBody()
            );
        }
    }
}
