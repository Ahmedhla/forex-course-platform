package com.forex.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    public void sendPasswordResetEmail(String to, String resetToken) {
        try {
            String resetLink = baseUrl + "/reset-password.html?token=" + resetToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("🔐 Password Reset Request - Forex Academy");
            message.setText(
                    "Hello,\n\n" +
                            "We received a request to reset your password for your Forex Academy account.\n\n" +
                            "Please click the link below to reset your password:\n" +
                            resetLink + "\n\n" +
                            "This link will expire in 1 hour.\n\n" +
                            "If you did not request this password reset, please ignore this email.\n\n" +
                            "For security reasons, do not share this link with anyone.\n\n" +
                            "Best regards,\n" +
                            "Forex Academy Team\n" +
                            "https://forex-academy.com"
            );
            message.setFrom("noreply@forexacademy.com");

            mailSender.send(message);
            System.out.println("✅ Password reset email sent successfully to: " + to);
            System.out.println("   Reset link: " + resetLink);
        } catch (Exception e) {
            System.err.println("❌ Failed to send password reset email to: " + to);
            System.err.println("   Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendWelcomeEmail(String to, String fullName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("🎉 Welcome to Forex Academy!");
            message.setText(
                    "Hello " + fullName + ",\n\n" +
                            "Welcome to Forex Academy! We're excited to have you on board.\n\n" +
                            "You can now access all our free forex trading courses.\n\n" +
                            "Get started by visiting our website and exploring our courses.\n\n" +
                            "If you have any questions, feel free to contact us.\n\n" +
                            "Happy trading!\n\n" +
                            "Best regards,\n" +
                            "Forex Academy Team"
            );
            message.setFrom("noreply@forexacademy.com");

            mailSender.send(message);
            System.out.println("✅ Welcome email sent to: " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send welcome email to: " + to);
        }
    }
}