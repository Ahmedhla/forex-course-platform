package com.forex.controller;

import com.forex.model.Course;
import com.forex.model.User;
import com.forex.model.Video;
import com.forex.repository.CourseRepository;
import com.forex.repository.UserRepository;
import com.forex.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${video.upload.path}")
    private String videoUploadPath;

    @Value("${thumbnail.upload.path}")
    private String thumbnailUploadPath;

    // ============================================
    // COURSE MANAGEMENT
    // ============================================

    // Create a new course (free by default)
    @PostMapping("/courses")
    public ResponseEntity<Map<String, Object>> createCourse(@RequestBody Course course) {
        try {
            course.setPrice(0.0);

            Course savedCourse = courseRepository.save(course);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", savedCourse.getId());
            response.put("title", savedCourse.getTitle());
            response.put("message", "Course created successfully");
            System.out.println("✅ Free course created: " + savedCourse.getTitle());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get all courses
    @GetMapping("/courses")
    public ResponseEntity<?> getAllCourses() {
        return ResponseEntity.ok(courseRepository.findAll());
    }

    // Get a specific course with videos
    @GetMapping("/courses/{id}")
    public ResponseEntity<?> getCourse(@PathVariable Long id) {
        return courseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update an existing course
    @PutMapping("/courses/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @RequestBody Course courseDetails) {
        try {
            Course course = courseRepository.findById(id).orElse(null);
            if (course == null) {
                return ResponseEntity.notFound().build();
            }

            course.setTitle(courseDetails.getTitle());
            course.setDescription(courseDetails.getDescription());
            course.setPrice(0.0);
            if (courseDetails.getThumbnailUrl() != null) {
                course.setThumbnailUrl(courseDetails.getThumbnailUrl());
            }

            Course updatedCourse = courseRepository.save(course);
            System.out.println("✅ Course updated: " + updatedCourse.getTitle());
            return ResponseEntity.ok(updatedCourse);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Delete a course (and all its videos)
    @Transactional
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        try {
            Course course = courseRepository.findById(id).orElse(null);
            if (course == null) {
                return ResponseEntity.notFound().build();
            }

            if (course.getVideos() != null && !course.getVideos().isEmpty()) {
                ArrayList<Video> videosToDelete = new ArrayList<>(course.getVideos());
                for (Video video : videosToDelete) {
                    videoRepository.delete(video);
                }
            }

            courseRepository.delete(course);
            System.out.println("✅ Course deleted: " + id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Course deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ============================================
    // VIDEO MANAGEMENT
    // ============================================

    // Add YouTube video to a course
    @PostMapping("/courses/{courseId}/videos")
    public ResponseEntity<?> addVideo(@PathVariable Long courseId, @RequestBody Video video) {
        try {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return ResponseEntity.notFound().build();
            }

            System.out.println("📹 Adding YouTube video to course: " + course.getTitle());
            System.out.println("   Video title: " + video.getTitle());
            System.out.println("   YouTube URL/ID: " + video.getVideoUrl());

            video.setCourse(course);
            video.setOrderNumber(course.getVideos().size() + 1);

            Video savedVideo = videoRepository.save(video);
            System.out.println("   ✅ YouTube video saved with ID: " + savedVideo.getId());

            return ResponseEntity.ok(savedVideo);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Delete a video
    @DeleteMapping("/videos/{videoId}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long videoId) {
        try {
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video == null) {
                return ResponseEntity.notFound().build();
            }

            videoRepository.delete(video);
            System.out.println("✅ Video deleted: " + videoId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Video deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ============================================
    // FILE UPLOADS
    // ============================================

    // Upload thumbnail image
    @PostMapping("/upload/thumbnail")
    public ResponseEntity<?> uploadThumbnail(@RequestParam("file") MultipartFile file) {
        try {
            File directory = new File(thumbnailUploadPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            Path path = Paths.get(thumbnailUploadPath + filename);
            Files.write(path, file.getBytes());

            String thumbnailUrl = "/uploads/thumbnails/" + filename;
            System.out.println("✅ Thumbnail saved: " + thumbnailUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("thumbnailUrl", thumbnailUrl);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ============================================
    // USER MANAGEMENT (for manage-users.html)
    // ============================================

    // Get all users (admin only)
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();

            // Remove passwords from response for security
            List<Map<String, Object>> safeUsers = new ArrayList<>();
            for (User user : users) {
                Map<String, Object> safeUser = new HashMap<>();
                safeUser.put("id", user.getId());
                safeUser.put("email", user.getEmail());
                safeUser.put("fullName", user.getFullName());
                safeUser.put("role", user.getRole());
                safeUser.put("createdAt", user.getCreatedAt());
                safeUsers.add(safeUser);
            }

            return ResponseEntity.ok(safeUsers);
        } catch (Exception e) {
            System.err.println("❌ Error fetching users: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Get a single user by ID
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> safeUser = new HashMap<>();
            safeUser.put("id", user.getId());
            safeUser.put("email", user.getEmail());
            safeUser.put("fullName", user.getFullName());
            safeUser.put("role", user.getRole());
            safeUser.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(safeUser);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Make a user admin
    @PutMapping("/users/{userId}/make-admin")
    public ResponseEntity<?> makeAdmin(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            user.setRole("ADMIN");
            userRepository.save(user);

            System.out.println("✅ User " + user.getEmail() + " is now an ADMIN");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User is now an admin");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Remove admin role (make user a student)
    @PutMapping("/users/{userId}/remove-admin")
    public ResponseEntity<?> removeAdmin(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            user.setRole("STUDENT");
            userRepository.save(user);

            System.out.println("✅ Admin role removed from: " + user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Admin role removed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Delete a user
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            // Prevent deleting the last admin
            long adminCount = userRepository.findAll().stream().filter(u -> "ADMIN".equals(u.getRole())).count();
            if ("ADMIN".equals(user.getRole()) && adminCount <= 1) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Cannot delete the last admin user");
                return ResponseEntity.badRequest().body(error);
            }

            userRepository.delete(user);
            System.out.println("✅ User deleted: " + user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Get user statistics
    @GetMapping("/users/stats")
    public ResponseEntity<?> getUserStats() {
        try {
            List<User> users = userRepository.findAll();

            long totalUsers = users.size();
            long adminCount = users.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
            long studentCount = users.stream().filter(u -> "STUDENT".equals(u.getRole())).count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("adminCount", adminCount);
            stats.put("studentCount", studentCount);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}