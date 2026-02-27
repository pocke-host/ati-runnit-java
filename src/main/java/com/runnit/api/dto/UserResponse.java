// ========== UserResponse.java ==========
package com.runnit.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String role;
    private boolean isVerified;
    private boolean isMentorAvailable;
    private Long followersCount;
    private Long followingCount;
}