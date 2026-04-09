package com.forex.controller;

import com.forex.model.Course;
import com.forex.model.Video;
import com.forex.repository.CourseRepository;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Value("${thumbnail.upload.path}")
    private String thumbnailUploadPath;

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

    // Delete a course
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
}