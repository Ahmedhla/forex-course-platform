package com.forex.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @JsonIgnore  // ← ADD THIS to hide password in JSON responses
    private String password;

    private String fullName;
    private String role;

    @ManyToMany
    @JoinTable(
            name = "user_courses",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @JsonIgnore  // ← ADD THIS to prevent infinite recursion
    private List<Course> purchasedCourses = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore  // ← ADD THIS
    private List<VideoProgress> videoProgresses = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public List<Course> getPurchasedCourses() { return purchasedCourses; }
    public void setPurchasedCourses(List<Course> purchasedCourses) { this.purchasedCourses = purchasedCourses; }
    public List<VideoProgress> getVideoProgresses() { return videoProgresses; }
    public void setVideoProgresses(List<VideoProgress> videoProgresses) { this.videoProgresses = videoProgresses; }
}