package com.example.bookstore.service;

import com.example.bookstore.dto.AddressDTO;
import com.example.bookstore.dto.UpdateProfileRequest;
import com.example.bookstore.dto.UserProfileResponse;
import com.example.bookstore.entity.Address;
import com.example.bookstore.entity.User;
import com.example.bookstore.repository.AddressRepository;
import com.example.bookstore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldGetProfileForAuthenticatedUser() {
        User user = User.builder()
                .userId(1L)
                .fullName("Nguyễn Văn A")
                .email("a@example.com")
                .phone("0123456789")
                .build();

        when(userRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByUserUserId(1L)).thenReturn(List.of());

        UserProfileResponse response = userService.getMyProfile(1L);

        assertEquals("Nguyễn Văn A", response.getFullName());
        assertEquals("a@example.com", response.getEmail());
        assertEquals("0123456789", response.getPhone());
        assertEquals(0, response.getAddresses().size());
    }

    @Test
    void shouldUpdateProfileForAuthenticatedUser() {
        User user = User.builder()
                .userId(2L)
                .fullName("Old Name")
                .email("old@example.com")
                .phone("0000000000")
                .build();

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("New Name");
        request.setEmail("new@example.com");
        request.setPhone("1111111111");
        request.setAddresses(List.of(
                AddressDTO.builder()
                        .receiverName("New Receiver")
                        .phone("1111111111")
                        .province("Hà Nội")
                        .district("Cầu Giấy")
                        .ward("Dịch Vọng")
                        .detailAddress("Số 123")
                        .isDefault(true)
                        .build()
        ));

        when(userRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(addressRepository.findByUserUserId(2L)).thenReturn(List.of(
                Address.builder().addressId(10L).user(user).receiverName("New Receiver").build()
        ));
        when(addressRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.updateMyProfile(2L, request);

        assertEquals("New Name", response.getFullName());
        assertEquals("new@example.com", response.getEmail());
        assertEquals("1111111111", response.getPhone());
        assertEquals(1, response.getAddresses().size());
        assertEquals("New Receiver", response.getAddresses().get(0).getReceiverName());
    }
}
