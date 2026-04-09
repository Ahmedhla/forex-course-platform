package com.forex.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class VideoStreamService {

    @Value("${video.upload.path}")
    private String videoPath;

    public Resource getVideoResource(String filename) {
        try {
            Path path = Paths.get(videoPath + filename);
            File file = path.toFile();
            if (file.exists() && file.isFile()) {
                return new FileSystemResource(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getContentType(String filename) {
        try {
            Path path = Paths.get(videoPath + filename);
            String contentType = Files.probeContentType(path);
            return contentType != null ? contentType : "video/mp4";
        } catch (IOException e) {
            return "video/mp4";
        }
    }

    public long getVideoLength(String filename) {
        File file = new File(videoPath + filename);
        return file.exists() ? file.length() : 0;
    }
}