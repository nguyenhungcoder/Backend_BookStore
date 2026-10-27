package com.example.bookstore.controller;

import com.example.bookstore.dto.AdminContactResponse;
import com.example.bookstore.dto.CreateContactRequest;
import com.example.bookstore.dto.UpdateContactStatusRequest;
import com.example.bookstore.entity.Contact;
import com.example.bookstore.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<Contact> createContact(@Valid @RequestBody CreateContactRequest request) {
        Contact contact = contactService.createContact(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(contact);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<AdminContactResponse>> getAllContacts(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.getAllContacts(keyword, pageable));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AdminContactResponse> updateContactStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContactStatusRequest request
    ) {
        return ResponseEntity.ok(contactService.updateContactStatus(id, request));
    }
}
