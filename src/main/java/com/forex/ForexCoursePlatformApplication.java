package com.forex;

import com.forex.model.User;
import com.forex.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class ForexCoursePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForexCoursePlatformApplication.class, args);
        System.out.println("Server started on http://localhost:8080");
    }

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail("admin@forex.com")) {
                User admin = new User();
                admin.setEmail("admin@forex.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFullName("Admin User");
                admin.setRole("ADMIN");
                userRepository.save(admin);
                System.out.println("✅ Admin created: admin@forex.com / admin123");
            }
        };
    }
}