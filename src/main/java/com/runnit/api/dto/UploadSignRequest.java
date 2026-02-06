// ========== UploadSignRequest.java ==========
package com.runnit.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UploadSignRequest {
    
    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name too long")
    private String fileName;
    
    @NotBlank(message = "Content type is required")
    @Pattern(regexp = "image/(jpeg|jpg|png|gif|webp)", message = "Only images allowed")
    private String contentType;
}