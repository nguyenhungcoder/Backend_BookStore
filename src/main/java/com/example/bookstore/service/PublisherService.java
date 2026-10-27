package com.example.bookstore.service;

import com.example.bookstore.dto.CreatePublisherRequest;
import com.example.bookstore.dto.PublisherResponse;
import com.example.bookstore.dto.UpdatePublisherRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublisherService {
    Page<PublisherResponse> getAllPublishers(Pageable pageable, String keyword);
    PublisherResponse getPublisherById(Long id);
    PublisherResponse createPublisher(CreatePublisherRequest request);
    PublisherResponse updatePublisher(Long id, UpdatePublisherRequest request);
    void deletePublisher(Long id);
}
