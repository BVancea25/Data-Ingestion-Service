package com.dataflow.dataingestionservice.Controllers;

import com.dataflow.dataingestionservice.Models.Category;
import com.dataflow.dataingestionservice.Services.CategoryService;
import com.dataflow.dataingestionservice.Utils.Constants.TransactionType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Create a new category
     */
    @PostMapping
    public ResponseEntity<Category> create(@RequestBody Category category) {
        Category created = categoryService.create(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing category
     */
    @PutMapping("/{id}")
    public ResponseEntity<Category> update(
            @PathVariable String id,
            @RequestBody Category category
    ) {
        Category updated = categoryService.update(id, category);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get all categories for current user
     * Optional filter by transaction type
     *
     * /api/categories
     * /api/categories?type=INCOME
     * /api/categories?type=EXPENSE
     */
    @GetMapping
    public ResponseEntity<List<Category>> getAll(
            @RequestParam(required = false) TransactionType type
    ) {
        List<Category> categories = (type == null)
                ? categoryService.getAll()
                : categoryService.getByType(type);

        return ResponseEntity.ok(categories);
    }

    /**
     * Get a single category by id
     */
    @GetMapping("/{id}")
    public ResponseEntity<Category> getById(@PathVariable String id) {
        Category category = categoryService.getById(id);
        return ResponseEntity.ok(category);
    }
}
