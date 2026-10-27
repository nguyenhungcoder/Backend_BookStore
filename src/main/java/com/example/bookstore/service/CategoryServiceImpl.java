package com.example.bookstore.service;

import com.example.bookstore.dto.CategoryResponse;
import com.example.bookstore.dto.CreateCategoryRequest;
import com.example.bookstore.dto.UpdateCategoryRequest;
import com.example.bookstore.entity.Category;
import com.example.bookstore.exception.DuplicateResourceException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategories(Pageable pageable, String keyword) {
        return categoryRepository.findAllActive(keyword, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findByIdAndNotDeleted(id);
        if (category == null) {
            throw new ResourceNotFoundException("Category not found");
        }
        return toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCaseAndStatusNot(request.getName(), "DELETED")) {
            throw new DuplicateResourceException("Category already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .status(request.getStatus())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findByIdAndNotDeleted(id);
        if (category == null) {
            throw new ResourceNotFoundException("Category not found");
        }

        if (request.getName() != null && !request.getName().equalsIgnoreCase(category.getName())
                && categoryRepository.existsByNameIgnoreCaseAndStatusNot(request.getName(), "DELETED")) {
            throw new DuplicateResourceException("Category already exists");
        }

        if (request.getName() != null) category.setName(request.getName());
        if (request.getSlug() != null) category.setSlug(request.getSlug());
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getStatus() != null) category.setStatus(request.getStatus());
        category.setUpdatedAt(LocalDateTime.now());

        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse uploadCategoryImage(Long id, MultipartFile file) {
        Category category = categoryRepository.findByIdAndNotDeleted(id);
        if (category == null) {
            throw new ResourceNotFoundException("Category not found");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        try {
            String uploadsDir = "uploads/categories";
            Path uploadPath = Paths.get(uploadsDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }
            String filename = System.currentTimeMillis() + "-" + UUID.randomUUID() + ext;
            Path filePath = uploadPath.resolve(filename);
            try (var input = file.getInputStream()) {
                Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            String dbPath = "/uploads/categories/" + filename;
            category.setImageUrl(dbPath);
            category.setUpdatedAt(LocalDateTime.now());

            return toResponse(categoryRepository.save(category));
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findByIdAndNotDeleted(id);
        if (category == null) {
            throw new ResourceNotFoundException("Category not found");
        }
        category.setStatus("DELETED");
        category.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .imageUrl(category.getImageUrl())
                .build();
    }
}
