package com.forex.repository;

import com.forex.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email (for login)
    Optional<User> findByEmail(String email);

    // Check if email exists (for registration)
    boolean existsByEmail(String email);

    // ============================================
    // NEW: Find user by reset token (for password reset)
    // ============================================
    Optional<User> findByResetToken(String resetToken);
}