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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileAddressTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private UserService userService;

    @Test
    void updateMyProfile_shouldReplaceAddressesFromNewPayload() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("demo");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Alice");
        request.setAddresses(List.of(
                AddressDTO.builder()
                        .receiverName("Alice Nguyen")
                        .phone("0123456789")
                        .province("Hà Nội")
                        .district("Cầu Giấy")
                        .ward("Dịch Vọng")
                        .detailAddress("Số 123")
                        .isDefault(true)
                        .build()
        ));

        when(userRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByUserUserId(1L)).thenReturn(List.of(Address.builder().addressId(99L).user(user).receiverName("Alice Nguyen").build()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(addressRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.updateMyProfile(1L, request);

        assertEquals(1, response.getAddresses().size());
        assertEquals("Alice Nguyen", response.getAddresses().get(0).getReceiverName());
        verify(addressRepository).deleteAll(anyList());
        verify(addressRepository).saveAll(anyList());
    }
}
