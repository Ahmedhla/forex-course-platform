package com.forex;

import com.forex.model.User;
import com.forex.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;

@SpringBootApplication
public class ForexCoursePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForexCoursePlatformApplication.class, args);
        System.out.println("========================================");
        System.out.println("🚀 Forex Course Platform Started!");
        System.out.println("========================================");
    }

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Create admin user if not exists
            if (!userRepository.existsByEmail("admin@forex.com")) {
                User admin = new User();
                admin.setEmail("admin@forex.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFullName("System Administrator");
                admin.setRole("ADMIN");
                admin.setCreatedAt(LocalDateTime.now());
                userRepository.save(admin);
                System.out.println("✅ Admin user created - Email: admin@forex.com, Password: admin123");
            } else {
                System.out.println("✅ Admin user already exists");
            }
        };
    }
}