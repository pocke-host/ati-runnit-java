package com.runnit.api.dto;

import jakarta.validation.constraints.*;

public class UploadSignRequest {

    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name too long")
    private String fileName;

    @NotBlank(message = "Content type is required")
    @Pattern(regexp = "image/(jpeg|jpg|png|gif|webp)", message = "Only images allowed")
    private String contentType;

    public UploadSignRequest() {}

    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }

    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
