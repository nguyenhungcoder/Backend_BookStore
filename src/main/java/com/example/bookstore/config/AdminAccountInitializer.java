package com.example.bookstore.config;

import com.example.bookstore.entity.User;
import com.example.bookstore.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class AdminAccountInitializer {

    @Bean
    public CommandLineRunner initAdminAccount(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminUsername = "admin@gmail.com";
            String adminEmail = "admin@gmail.com";
            String adminPassword = "admin123";

            User admin = userRepository.findByUsername(adminUsername)
                    .or(() -> userRepository.findByEmail(adminEmail))
                    .orElse(null);
            if (admin == null) {
                admin = User.builder()
                        .username(adminUsername)
                        .passwordHash(passwordEncoder.encode(adminPassword))
                        .email(adminEmail)
                        .phone("0999999999")
                        .fullName("System Administrator")
                        .role("ADMIN")
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
            } else {
                admin.setUsername(adminUsername);
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setEmail(adminEmail);
                admin.setPhone("0999999999");
                admin.setFullName("System Administrator");
                admin.setRole("ADMIN");
                admin.setStatus("ACTIVE");
                admin.setUpdatedAt(LocalDateTime.now());
            }

            userRepository.save(admin);
        };
    }
}
