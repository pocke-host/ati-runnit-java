package com.runnit.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class EmergencyContactDTO {
    private Long id;
    @NotBlank
    private String name;
    private String phone;
    private String email;
    private String relationship;
    private Instant createdAt;
}
