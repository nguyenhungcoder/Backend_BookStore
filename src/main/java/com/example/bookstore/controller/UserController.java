package com.example.bookstore.controller;

import com.example.bookstore.dto.UpdateProfileRequest;
import com.example.bookstore.dto.UpdateUserStatusRequest;
import com.example.bookstore.dto.UserProfileResponse;
import com.example.bookstore.dto.UserResponse;
import com.example.bookstore.security.UserPrincipal;
import com.example.bookstore.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/profile", "/me"})
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getMyProfile(principal.getUserId()));
    }

    @PutMapping({"/profile", "/me"})
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateMyProfile(principal.getUserId(), request));
    }

    // Admin endpoints
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) String keyword, // ❤️ Mở cửa hứng biến keyword
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // Truyền keyword xuống cho UserService
        Page<UserResponse> response = userService.getAllUsers(keyword, pageable);
        return ResponseEntity.ok(response);
    }
    

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        UserResponse response = userService.updateUserStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/upload-avatar")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserResponse> uploadUserAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        UserResponse response = userService.uploadUserAvatar(id, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
