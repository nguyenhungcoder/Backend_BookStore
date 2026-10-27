package com.example.bookstore.service;

import com.example.bookstore.dto.AuthorResponse;
import com.example.bookstore.dto.CreateAuthorRequest;
import com.example.bookstore.dto.UpdateAuthorRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthorService {
    Page<AuthorResponse> getAllAuthors(Pageable pageable, String keyword);
    AuthorResponse getAuthorById(Long id);
    AuthorResponse createAuthor(CreateAuthorRequest request);
    AuthorResponse updateAuthor(Long id, UpdateAuthorRequest request);
    void deleteAuthor(Long id);
}
