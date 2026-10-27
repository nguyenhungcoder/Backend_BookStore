package com.example.bookstore.repository;

import com.example.bookstore.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserUserId(Long userId);

    Optional<Address> findByAddressIdAndUserUserId(Long addressId, Long userId);
}
