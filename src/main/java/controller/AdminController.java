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

    @GetMapping("/courses")
    public ResponseEntity<?> getAllCourses() {
        return ResponseEntity.ok(courseRepository.findAll());
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<?> getCourse(@PathVariable Long id) {
        Course course = courseRepository.findById(id).orElse(null);
        if (course == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", course.getId());
        response.put("title", course.getTitle());
        response.put("description", course.getDescription());
        response.put("price", course.getPrice());
        response.put("thumbnailUrl", course.getThumbnailUrl());
        response.put("videos", course.getVideos());
        response.put("pdfUrl", course.getPdfUrl());
        response.put("pdfTitle", course.getPdfTitle());
        response.put("pdfSize", course.getPdfSize());

        return ResponseEntity.ok(response);
    }

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
            return ResponseEntity.ok(updatedCourse);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

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

    @PostMapping("/courses/{courseId}/videos")
    public ResponseEntity<?> addVideo(@PathVariable Long courseId, @RequestBody Video video) {
        try {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return ResponseEntity.notFound().build();
            }
            video.setCourse(course);
            video.setOrderNumber(course.getVideos().size() + 1);
            Video savedVideo = videoRepository.save(video);
            return ResponseEntity.ok(savedVideo);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/videos/{videoId}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long videoId) {
        try {
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video == null) {
                return ResponseEntity.notFound().build();
            }
            videoRepository.delete(video);
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
    // PDF MATERIAL MANAGEMENT
    // ============================================

    @PostMapping("/courses/{courseId}/upload-pdf")
    public ResponseEntity<?> uploadCoursePdf(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file) {
        try {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return ResponseEntity.notFound().build();
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are allowed"));
            }

            File pdfDirectory = new File("./uploads/pdfs/");
            if (!pdfDirectory.exists()) {
                pdfDirectory.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String filename = UUID.randomUUID().toString() + "_" + originalFilename;
            String filePath = "./uploads/pdfs/" + filename;
            Files.write(Paths.get(filePath), file.getBytes());

            String pdfUrl = "/uploads/pdfs/" + filename;
            String pdfTitle = originalFilename.replace(".pdf", "");

            course.setPdfUrl(pdfUrl);
            course.setPdfTitle(pdfTitle);
            course.setPdfSize(file.getSize());
            courseRepository.save(course);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("pdfUrl", pdfUrl);
            response.put("pdfTitle", pdfTitle);
            response.put("pdfSize", file.getSize());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/courses/{courseId}/delete-pdf")
    public ResponseEntity<?> deleteCoursePdf(@PathVariable Long courseId) {
        try {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null || course.getPdfUrl() == null) {
                return ResponseEntity.notFound().build();
            }

            String filePath = "." + course.getPdfUrl();
            File pdfFile = new File(filePath);
            if (pdfFile.exists()) {
                pdfFile.delete();
            }

            course.setPdfUrl(null);
            course.setPdfTitle(null);
            course.setPdfSize(null);
            courseRepository.save(course);

            return ResponseEntity.ok(Map.of("success", true, "message", "PDF deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/courses/{courseId}/pdf-info")
    public ResponseEntity<?> getCoursePdfInfo(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("hasPdf", course.getPdfUrl() != null);
        response.put("pdfUrl", course.getPdfUrl());
        response.put("pdfTitle", course.getPdfTitle());
        response.put("pdfSize", course.getPdfSize());
        return ResponseEntity.ok(response);
    }

    // ============================================
    // FILE UPLOADS
    // ============================================

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
            return ResponseEntity.ok(Map.of("success", true, "thumbnailUrl", thumbnailUrl));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================
    // USER MANAGEMENT
    // ============================================

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}/make-admin")
    public ResponseEntity<?> makeAdmin(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            user.setRole("ADMIN");
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "User is now an admin"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}/remove-admin")
    public ResponseEntity<?> removeAdmin(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            user.setRole("STUDENT");
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "Admin role removed"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}