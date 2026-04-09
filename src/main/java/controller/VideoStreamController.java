package com.forex.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads/videos")
public class VideoStreamController {

    @Value("${video.upload.path}")
    private String videoPath;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<byte[]> streamVideo(@PathVariable String filename,
                                              @RequestHeader(value = "Range", required = false) String range) {

        System.out.println("=== Video Request ===");
        System.out.println("Filename: " + filename);

        try {
            // Build the full file path
            Path filePath = Paths.get(videoPath).resolve(filename).normalize();
            File videoFile = filePath.toFile();

            System.out.println("Full path: " + filePath.toAbsolutePath());
            System.out.println("File exists: " + videoFile.exists());
            System.out.println("File size: " + (videoFile.exists() ? videoFile.length() : 0) + " bytes");

            if (!videoFile.exists() || !videoFile.isFile()) {
                return ResponseEntity.notFound().build();
            }

            if (videoFile.length() == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "video/mp4";
            }

            long fileLength = videoFile.length();

            // If no range header, return full video
            if (range == null) {
                System.out.println("Serving full video file");
                byte[] fullVideo = Files.readAllBytes(filePath);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileLength))
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .body(fullVideo);
            }

            // Handle range request (for seeking)
            System.out.println("Range request: " + range);
            String[] ranges = range.replace("bytes=", "").split("-");
            long start = Long.parseLong(ranges[0]);
            long end = ranges.length > 1 ? Long.parseLong(ranges[1]) : fileLength - 1;

            if (end > fileLength - 1) {
                end = fileLength - 1;
            }

            long contentLength = end - start + 1;
            System.out.println("Serving bytes " + start + "-" + end + " (length: " + contentLength + ")");

            // Read the specific range of bytes
            RandomAccessFile randomAccessFile = new RandomAccessFile(videoFile, "r");
            randomAccessFile.seek(start);
            byte[] buffer = new byte[(int) contentLength];
            randomAccessFile.read(buffer, 0, (int) contentLength);
            randomAccessFile.close();

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(buffer);

        } catch (IOException e) {
            System.err.println("Error serving video: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}