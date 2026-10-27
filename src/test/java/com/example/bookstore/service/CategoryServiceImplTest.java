package com.example.bookstore.service;

import com.example.bookstore.dto.CreateCategoryRequest;
import com.example.bookstore.exception.DuplicateResourceException;
import com.example.bookstore.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void createCategory_shouldThrowWhenNameAlreadyExists() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Fantasy")
                .slug("fantasy")
                .status("ACTIVE")
                .build();

        when(categoryRepository.existsByNameIgnoreCaseAndStatusNot("Fantasy", "DELETED")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> categoryService.createCategory(request));
    }
}
