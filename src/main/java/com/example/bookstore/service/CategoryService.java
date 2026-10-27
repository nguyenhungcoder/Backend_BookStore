package com.example.bookstore.service;

import com.example.bookstore.dto.CategoryResponse;
import com.example.bookstore.dto.CreateCategoryRequest;
import com.example.bookstore.dto.UpdateCategoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface CategoryService {
    Page<CategoryResponse> getAllCategories(Pageable pageable, String keyword);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse createCategory(CreateCategoryRequest request);
    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);
    CategoryResponse uploadCategoryImage(Long id, MultipartFile file);
    void deleteCategory(Long id);
}
