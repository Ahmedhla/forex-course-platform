package com.forex.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "videos")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "youtube_id")
    private String youtubeId;

    private Integer duration;
    private Integer orderNumber;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonIgnore
    private Course course;

    // Constructors
    public Video() {}

    public Video(String title, String videoUrl, Integer duration) {
        this.title = title;
        this.videoUrl = videoUrl;
        this.duration = duration;
        this.youtubeId = extractYouTubeId(videoUrl);
    }

    // Helper method to extract YouTube ID from URL
    private String extractYouTubeId(String url) {
        if (url == null) return null;

        // If it's already just an ID (11 characters)
        if (url.matches("^[a-zA-Z0-9_-]{11}$")) {
            return url;
        }

        // Handle youtu.be format
        if (url.contains("youtu.be/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }

        // Handle youtube.com/watch?v= format
        if (url.contains("youtube.com/watch?v=")) {
            String[] parts = url.split("v=");
            if (parts.length > 1) {
                String id = parts[1];
                if (id.contains("&")) {
                    id = id.substring(0, id.indexOf("&"));
                }
                return id;
            }
        }

        // Handle youtube.com/embed/ format
        if (url.contains("youtube.com/embed/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }

        return url;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
        this.youtubeId = extractYouTubeId(videoUrl);
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public void setYoutubeId(String youtubeId) {
        this.youtubeId = youtubeId;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}