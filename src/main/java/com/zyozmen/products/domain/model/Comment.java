package com.zyozmen.products.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Comentario reciente asociado al producto.
 */
public class Comment {

    private String commentId;
    private String userId;
    private String username;
    private Integer rating;
    private String title;
    private String body;
    private Instant createdAt;

    public Comment() {
    }

    public Comment(String commentId, String userId, String username, Integer rating, String title, String body, Instant createdAt) {
        this.commentId = commentId;
        this.userId = userId;
        this.username = username;
        this.rating = rating;
        this.title = title;
        this.body = body;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return Objects.equals(commentId, comment.commentId)
                && Objects.equals(userId, comment.userId)
                && Objects.equals(username, comment.username)
                && Objects.equals(rating, comment.rating)
                && Objects.equals(title, comment.title)
                && Objects.equals(body, comment.body)
                && Objects.equals(createdAt, comment.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commentId, userId, username, rating, title, body, createdAt);
    }

    @Override
    public String toString() {
        return "Comment{" +
                "commentId='" + commentId + '\'' +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", rating=" + rating +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {

        private String commentId;
        private String userId;
        private String username;
        private Integer rating;
        private String title;
        private String body;
        private Instant createdAt;

        public Builder commentId(String commentId) {
            this.commentId = commentId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder rating(Integer rating) {
            this.rating = rating;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Comment build() {
            return new Comment(commentId, userId, username, rating, title, body, createdAt);
        }
    }
}
