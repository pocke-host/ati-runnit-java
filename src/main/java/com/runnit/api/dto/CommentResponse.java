package com.runnit.api.dto;

import java.time.Instant;

public class CommentResponse {
    private Long id;
    private String text;
    private Instant createdAt;
    private UserInfo user;

    public CommentResponse() {}

    public Long getId() { return id; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
    public UserInfo getUser() { return user; }

    public void setId(Long id) { this.id = id; }
    public void setText(String text) { this.text = text; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUser(UserInfo user) { this.user = user; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String text;
        private Instant createdAt;
        private UserInfo user;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder text(String text) { this.text = text; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder user(UserInfo user) { this.user = user; return this; }

        public CommentResponse build() {
            CommentResponse r = new CommentResponse();
            r.id = this.id;
            r.text = this.text;
            r.createdAt = this.createdAt;
            r.user = this.user;
            return r;
        }
    }

    public static class UserInfo {
        private Long id;
        private String displayName;
        private String avatarUrl;

        public UserInfo() {}

        public Long getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getAvatarUrl() { return avatarUrl; }

        public void setId(Long id) { this.id = id; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

        public static UserInfoBuilder builder() { return new UserInfoBuilder(); }

        public static class UserInfoBuilder {
            private Long id;
            private String displayName;
            private String avatarUrl;

            public UserInfoBuilder id(Long id) { this.id = id; return this; }
            public UserInfoBuilder displayName(String displayName) { this.displayName = displayName; return this; }
            public UserInfoBuilder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }

            public UserInfo build() {
                UserInfo u = new UserInfo();
                u.id = this.id;
                u.displayName = this.displayName;
                u.avatarUrl = this.avatarUrl;
                return u;
            }
        }
    }
}
