package com.example.bookstore.service;

import com.example.bookstore.dto.AddressDTO;
import com.example.bookstore.dto.UpdateProfileRequest;
import com.example.bookstore.dto.UpdateUserStatusRequest;
import com.example.bookstore.dto.UserProfileResponse;
import com.example.bookstore.dto.UserResponse;
import com.example.bookstore.entity.Address;
import com.example.bookstore.entity.User;
import com.example.bookstore.exception.BadRequestException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.AddressRepository;
import com.example.bookstore.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final FileUploadService fileUploadService;

    public UserService(UserRepository userRepository, AddressRepository addressRepository, FileUploadService fileUploadService) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.fileUploadService = fileUploadService;
    }

    // Admin methods
    // Thêm String keyword vào tham số
    // Nhớ thêm String keyword vào tham số của hàm
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String keyword, Pageable pageable) {
        // ❤️ Thay vì gọi findAll(), hãy gọi hàm searchUsers để dò tìm
        return userRepository.searchUsers(keyword, pageable)
                .map(this::toUserResponse); // (Giữ nguyên logic map dữ liệu cũ của em)
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        user.setStatus(request.getStatus());
        User updatedUser = userRepository.save(user);
        return toUserResponse(updatedUser);
    }

    @Transactional
    public UserResponse uploadUserAvatar(Long userId, MultipartFile file) {
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        String avatarPath = fileUploadService.uploadFile(file, "avatars");
        user.setAvatar(avatarPath);
        User updatedUser = userRepository.save(user);
        return toUserResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateUniqueProfileFields(userId, request);

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        List<AddressDTO> savedAddresses = null;
        if (request.getAddresses() != null) {
            savedAddresses = replaceUserAddresses(user, request.getAddresses());
        }

        User updatedUser = userRepository.save(user);
        return toProfileResponse(updatedUser, savedAddresses);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    private void validateUniqueProfileFields(Long currentUserId, UpdateProfileRequest request) {
        if (StringUtils.hasText(request.getEmail())) {
            userRepository.findByEmail(request.getEmail())
                    .filter(existingUser -> !existingUser.getUserId().equals(currentUserId))
                    .ifPresent(existingUser -> {
                        throw new BadRequestException("Email already exists");
                    });
        }

        if (StringUtils.hasText(request.getPhone())) {
            userRepository.findByPhone(request.getPhone())
                    .filter(existingUser -> !existingUser.getUserId().equals(currentUserId))
                    .ifPresent(existingUser -> {
                        throw new BadRequestException("Phone already exists");
                    });
        }
    }

    private List<AddressDTO> replaceUserAddresses(User user, List<AddressDTO> addresses) {
        List<Address> existingAddresses = addressRepository.findByUserUserId(user.getUserId());
        if (!existingAddresses.isEmpty()) {
            addressRepository.deleteAll(existingAddresses);
        }

        if (addresses == null || addresses.isEmpty()) {
            return List.of();
        }

        List<Address> newAddresses = new ArrayList<>();
        for (AddressDTO dto : addresses) {
            Address address = Address.builder()
                    .user(user)
                    .receiverName(dto.getReceiverName())
                    .phone(dto.getPhone())
                    .province(dto.getProvince())
                    .district(dto.getDistrict())
                    .ward(dto.getWard())
                    .detailAddress(dto.getDetailAddress())
                    .isDefault(Boolean.TRUE.equals(dto.getIsDefault()))
                    .build();
            newAddresses.add(address);
        }

        addressRepository.saveAll(newAddresses);
        return addresses;
    }

    private UserProfileResponse toProfileResponse(User user) {
        return toProfileResponse(user, null);
    }

    private UserProfileResponse toProfileResponse(User user, List<AddressDTO> fallbackAddresses) {
        List<AddressDTO> addressDTOs = fallbackAddresses;
        if (addressDTOs == null) {
            addressDTOs = addressRepository.findByUserUserId(user.getUserId())
                    .stream()
                    .map(this::toAddressDTO)
                    .toList();
        }

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .addresses(addressDTOs)
                .build();
    }

    private AddressDTO toAddressDTO(Address address) {
        return AddressDTO.builder()
                .receiverName(address.getReceiverName())
                .phone(address.getPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .detailAddress(address.getDetailAddress())
                .isDefault(address.getIsDefault())
                .build();
    }
}
