package com.runnit.api.dto;

import java.time.LocalDateTime;

public class UserResponseDTO {
    private Long id;
    private String email;
    private String fullName;
    private String profilePictureUrl;
    private String bio;
    private LocalDateTime createdAt;

    // Constructor for convenience
    public UserResponseDTO(Long id, String email, String fullName, String profilePictureUrl, String bio, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.profilePictureUrl = profilePictureUrl;
        this.bio = bio;
        this.createdAt = createdAt;
    }

    // Getters only (responses don’t need setters)
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public String getBio() { return bio; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
