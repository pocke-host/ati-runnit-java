// ========== S3Service.java ==========
package com.runnit.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {
    
    private final S3Presigner s3Presigner;
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    
    @Value("${aws.s3.region}")
    private String region;
    
    public Map<String, String> generatePresignedUploadUrl(String fileName, String contentType) {
        String key = "uploads/" + UUID.randomUUID() + "-" + fileName;
        
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();
        
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest)
                .build();
        
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        
        String uploadUrl = presignedRequest.url().toString();
        String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
        
        Map<String, String> response = new HashMap<>();
        response.put("uploadUrl", uploadUrl);
        response.put("fileUrl", fileUrl);
        response.put("key", key);
        
        return response;
    }
    
    public String getPublicUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }
}