// ========== UploadController.java ==========
package com.runnit.api.controller;

import com.runnit.api.dto.UploadSignRequest;
import com.runnit.api.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {
    
    private final S3Service s3Service;
    
    @PostMapping("/sign")
    public ResponseEntity<?> signUpload(@Valid @RequestBody UploadSignRequest request) {
        try {
            Map<String, String> response = s3Service.generatePresignedUploadUrl(
                request.getFileName(),
                request.getContentType()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}