package com.forex.controller;

import com.forex.model.Course;
import com.forex.model.User;
import com.forex.model.Video;
import com.forex.model.VideoProgress;
import com.forex.repository.CourseRepository;
import com.forex.repository.UserRepository;
import com.forex.repository.VideoProgressRepository;
import com.forex.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private VideoProgressRepository videoProgressRepository;

    // Get all public courses
    @GetMapping("/public")
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseRepository.findAll());
    }

    // Get a specific course by ID
    @GetMapping("/public/{id}")
    public ResponseEntity<Course> getCourse(@PathVariable Long id) {
        return courseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get my courses (all courses are free)
    @GetMapping("/my-courses")
    public ResponseEntity<List<Course>> getMyCourses() {
        return ResponseEntity.ok(courseRepository.findAll());
    }

    // Get videos for a course
    @GetMapping("/{courseId}/videos")
    public ResponseEntity<?> getCourseVideos(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(course.getVideos());
    }

    // Update video progress
    @PostMapping("/progress/{videoId}")
    public ResponseEntity<?> updateProgress(@PathVariable Long videoId, @RequestBody Map<String, Integer> request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        Video video = videoRepository.findById(videoId).orElse(null);

        if (user == null || video == null) {
            return ResponseEntity.badRequest().build();
        }

        VideoProgress progress = videoProgressRepository.findByUserAndVideo(user, video)
                .orElse(new VideoProgress());

        progress.setUser(user);
        progress.setVideo(video);
        progress.setLastPosition(request.get("position"));

        if (video.getDuration() != null && request.get("position") >= (video.getDuration() - 5)) {
            progress.setCompleted(true);
        }

        videoProgressRepository.save(progress);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Get video progress
    @GetMapping("/progress/{videoId}")
    public ResponseEntity<?> getProgress(@PathVariable Long videoId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        Video video = videoRepository.findById(videoId).orElse(null);

        if (user == null || video == null) {
            return ResponseEntity.ok(Map.of("position", 0));
        }

        VideoProgress progress = videoProgressRepository.findByUserAndVideo(user, video).orElse(null);
        int position = progress != null && progress.getLastPosition() != null ? progress.getLastPosition() : 0;

        return ResponseEntity.ok(Map.of("position", position, "completed", progress != null && progress.getCompleted()));
    }
}