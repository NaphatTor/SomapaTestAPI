package com.example.flight.controller;

import com.example.flight.domain.UploadResponse;
import com.example.flight.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;

@RestController
public class FileController {

    private final FileService manager;

    public FileController(FileService manager) {
        this.manager = manager;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam("flightNo") String flightNo) {
    	System.out.println("Flight No "+flightNo);
    	System.out.println("File : "+file.getOriginalFilename());
        try {
            UploadResponse res = manager.fileUpload(file, flightNo);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam("filename") String filename) {
        try {
            Resource res = manager.fileDownload(filename);
            if (res == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

            String contentType = Files.probeContentType(res.getFile().toPath());
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(res.getFilename(), java.nio.charset.StandardCharsets.UTF_8)
                                    .toString())
                    .body(res);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
