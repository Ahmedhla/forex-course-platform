package com.forex.controller;

import com.forex.model.User;
import com.forex.repository.UserRepository;
import com.forex.security.JwtUtil;
import com.forex.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private EmailService emailService;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

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
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(email, fullName);
        } catch (Exception e) {
            System.out.println("Welcome email failed, but user was created: " + e.getMessage());
        }

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
    // FORGOT PASSWORD - REQUEST RESET
    // ============================================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        User user = userRepository.findByEmail(email).orElse(null);

        // For security, always return success even if email doesn't exist
        // This prevents email enumeration attacks
        if (user == null) {
            System.out.println("Password reset requested for non-existent email: " + email);
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

        // Send email with reset link
        try {
            emailService.sendPasswordResetEmail(email, resetToken);
            System.out.println("✅ Password reset email sent to: " + email);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email to: " + email);
            e.printStackTrace();
        }

        return ResponseEntity.ok(Map.of(
                "message", "Password reset link has been sent to your email address."
        ));
    }

    // ============================================
    // RESET PASSWORD - VERIFY TOKEN AND UPDATE
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

        System.out.println("✅ Password reset successfully for user: " + user.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "Password has been reset successfully. You can now login with your new password."
        ));
    }

    // ============================================
    // VALIDATE RESET TOKEN
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

    // ============================================
    // GET CURRENT USER INFO
    // ============================================
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("role", user.getRole());
        response.put("createdAt", user.getCreatedAt());

        return ResponseEntity.ok(response);
    }
}