package com.forex;

import com.forex.model.User;
import com.forex.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableJpaAuditing
public class MainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
        System.out.println("========================================");
        System.out.println("🚀 Forex Course Platform Started!");
        System.out.println("📺 YouTube Course Platform with PostgreSQL");
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
                userRepository.save(admin);
                System.out.println("✅ Admin user created - Email: admin@forex.com, Password: admin123");
            } else {
                System.out.println("✅ Admin user already exists");
            }

            // Create a demo student user if not exists (optional)
            if (!userRepository.existsByEmail("student@demo.com")) {
                User student = new User();
                student.setEmail("student@demo.com");
                student.setPassword(passwordEncoder.encode("student123"));
                student.setFullName("Demo Student");
                student.setRole("STUDENT");
                userRepository.save(student);
                System.out.println("✅ Demo student created - Email: student@demo.com, Password: student123");
            }

            // Print total user count
            long userCount = userRepository.count();
            System.out.println("📊 Total users in database: " + userCount);
        };
    }
}