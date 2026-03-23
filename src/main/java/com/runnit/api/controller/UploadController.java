package com.runnit.api.controller;

import com.runnit.api.dto.UploadSignRequest;
import com.runnit.api.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final S3Service s3Service;

    @PostMapping("/sign")
    public ResponseEntity<?> signUpload(
            @Valid @RequestBody UploadSignRequest request,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            log.debug("S3 presign request: userId={} fileName={} contentType={}",
                    userId, request.getFileName(), request.getContentType());
            Map<String, String> response = s3Service.generatePresignedUploadUrl(
                    request.getFileName(),
                    request.getContentType()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            log.error("Failed to generate presigned upload URL: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
