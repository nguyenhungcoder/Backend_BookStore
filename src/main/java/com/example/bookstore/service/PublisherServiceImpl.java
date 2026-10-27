package com.example.bookstore.service;

import com.example.bookstore.dto.CreatePublisherRequest;
import com.example.bookstore.dto.PublisherResponse;
import com.example.bookstore.dto.UpdatePublisherRequest;
import com.example.bookstore.entity.Publisher;
import com.example.bookstore.exception.DuplicateResourceException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.PublisherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {

    private final PublisherRepository publisherRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PublisherResponse> getAllPublishers(Pageable pageable, String keyword) {
        return publisherRepository.findAllActive(keyword, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PublisherResponse getPublisherById(Long id) {
        Publisher publisher = publisherRepository.findByIdAndNotDeleted(id);
        if (publisher == null) {
            throw new ResourceNotFoundException("Publisher not found");
        }
        return toResponse(publisher);
    }

    @Override
    @Transactional
    public PublisherResponse createPublisher(CreatePublisherRequest request) {
        if (publisherRepository.existsByNameIgnoreCaseAndStatusNot(request.getName(), "DELETED")) {
            throw new DuplicateResourceException("Publisher already exists");
        }

        Publisher publisher = Publisher.builder()
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .website(request.getWebsite())
                .status(request.getStatus())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(publisherRepository.save(publisher));
    }

    @Override
    @Transactional
    public PublisherResponse updatePublisher(Long id, UpdatePublisherRequest request) {
        Publisher publisher = publisherRepository.findByIdAndNotDeleted(id);
        if (publisher == null) {
            throw new ResourceNotFoundException("Publisher not found");
        }

        if (request.getName() != null && !request.getName().equalsIgnoreCase(publisher.getName())
                && publisherRepository.existsByNameIgnoreCaseAndStatusNot(request.getName(), "DELETED")) {
            throw new DuplicateResourceException("Publisher already exists");
        }

        if (request.getName() != null) publisher.setName(request.getName());
        if (request.getAddress() != null) publisher.setAddress(request.getAddress());
        if (request.getPhone() != null) publisher.setPhone(request.getPhone());
        if (request.getWebsite() != null) publisher.setWebsite(request.getWebsite());
        if (request.getStatus() != null) publisher.setStatus(request.getStatus());
        publisher.setUpdatedAt(LocalDateTime.now());

        return toResponse(publisherRepository.save(publisher));
    }

    @Override
    @Transactional
    public void deletePublisher(Long id) {
        Publisher publisher = publisherRepository.findByIdAndNotDeleted(id);
        if (publisher == null) {
            throw new ResourceNotFoundException("Publisher not found");
        }
        publisher.setStatus("DELETED");
        publisher.setUpdatedAt(LocalDateTime.now());
        publisherRepository.save(publisher);
    }

    private PublisherResponse toResponse(Publisher publisher) {
        return PublisherResponse.builder()
                .publisherId(publisher.getPublisherId())
                .name(publisher.getName())
                .address(publisher.getAddress())
                .phone(publisher.getPhone())
                .website(publisher.getWebsite())
                .status(publisher.getStatus())
                .createdAt(publisher.getCreatedAt())
                .updatedAt(publisher.getUpdatedAt())
                .build();
    }
}
