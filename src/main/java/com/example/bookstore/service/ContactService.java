package com.example.bookstore.service;

import com.example.bookstore.dto.AdminContactResponse;
import com.example.bookstore.dto.CreateContactRequest;
import com.example.bookstore.dto.UpdateContactStatusRequest;
import com.example.bookstore.entity.Contact;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.ContactRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ContactService {

    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Transactional
    public Contact createContact(CreateContactRequest request) {
        Contact contact = Contact.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .subject(request.getSubject())
                .message(request.getMessage())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        return contactRepository.save(contact);
    }

    // Admin methods
    @Transactional(readOnly = true)
    public Page<AdminContactResponse> getAllContacts(String keyword, Pageable pageable) {
        return contactRepository.findAllActive(keyword, pageable)
                .map(this::toAdminContactResponse);
    }

    @Transactional
    public AdminContactResponse updateContactStatus(Long contactId, UpdateContactStatusRequest request) {
        Contact contact = contactRepository.findByIdAndNotDeleted(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        contact.setStatus(request.getStatus());
        Contact updatedContact = contactRepository.save(contact);
        return toAdminContactResponse(updatedContact);
    }

    private AdminContactResponse toAdminContactResponse(Contact contact) {
        return AdminContactResponse.builder()
                .contactId(contact.getContactId())
                .fullName(contact.getFullName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .subject(contact.getSubject())
                .message(contact.getMessage())
                .status(contact.getStatus())
                .createdAt(contact.getCreatedAt())
                .build();
    }
}
