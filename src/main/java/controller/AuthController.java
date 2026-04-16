package com.forex.controller;

import com.forex.model.User;
import com.forex.repository.UserRepository;
import com.forex.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // ============================================
    // REGISTER
    // ============================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        String fullName = request.get("fullName");

        // Validate input
        if (email == null || password == null || fullName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required"));
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        // Create new user
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole("STUDENT");

        userRepository.save(user);

        // Generate token
        String token = jwtUtil.generateToken(email, "STUDENT");

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", "STUDENT");
        response.put("fullName", fullName);

        return ResponseEntity.ok(response);
    }

    // ============================================
    // LOGIN
    // ============================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        String token = jwtUtil.generateToken(email, user.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole());
        response.put("fullName", user.getFullName());

        return ResponseEntity.ok(response);
    }

    // ============================================
    // FORGOT PASSWORD - STEP 1: REQUEST RESET
    // ============================================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        User user = userRepository.findByEmail(email).orElse(null);

        // For security, always return success even if email doesn't exist
        // This prevents attackers from finding out which emails are registered
        if (user == null) {
            return ResponseEntity.ok(Map.of(
                    "message", "If your email is registered, you will receive a password reset link."
            ));
        }

        // Generate unique reset token
        String resetToken = UUID.randomUUID().toString();

        // Save token with 1 hour expiration
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        userRepository.save(user);

        // Build reset link
        String resetLink = "https://forex-course-platform.onrender.com/reset-password.html?token=" + resetToken;

        // For local testing, use localhost
        // String resetLink = "http://localhost:8080/reset-password.html?token=" + resetToken;

        System.out.println("========================================");
        System.out.println("PASSWORD RESET REQUEST");
        System.out.println("Email: " + email);
        System.out.println("Reset Token: " + resetToken);
        System.out.println("Reset Link: " + resetLink);
        System.out.println("Expires: " + user.getResetTokenExpiry());
        System.out.println("========================================");

        // TODO: In production, send actual email here
        // For now, we return the link in the response for testing
        // Remove this in production and implement email sending

        return ResponseEntity.ok(Map.of(
                "message", "Password reset link has been sent to your email.",
                "resetLink", resetLink  // Remove this line in production!
        ));
    }

    // ============================================
    // RESET PASSWORD - STEP 2: VERIFY TOKEN AND UPDATE
    // ============================================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        // Validate input
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reset token is required"));
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password is required"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }

        // Find user by reset token
        User user = userRepository.findByResetToken(token).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid or expired reset token. Please request a new password reset."
            ));
        }

        // Check if token is expired
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Reset token has expired. Please request a new password reset."
            ));
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));

        // Clear reset token fields
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Password has been reset successfully. You can now login with your new password."
        ));
    }

    // ============================================
    // VALIDATE RESET TOKEN - Check if token is valid
    // ============================================
    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Token is required"));
        }

        User user = userRepository.findByResetToken(token).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Invalid reset token"));
        }

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Reset token has expired"));
        }

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "email", user.getEmail()
        ));
    }
}