package com.zyozmen.products.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CommentTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        Instant createdAt = Instant.parse("2026-06-15T14:30:00Z");

        Comment comment = Comment.builder()
                .commentId("comment-1")
                .userId("user-1")
                .username("JohnDoe92")
                .rating(5)
                .title("Amazing sound quality")
                .body("The battery lasts forever")
                .createdAt(createdAt)
                .build();

        assertThat(comment.getCommentId()).isEqualTo("comment-1");
        assertThat(comment.getUserId()).isEqualTo("user-1");
        assertThat(comment.getUsername()).isEqualTo("JohnDoe92");
        assertThat(comment.getRating()).isEqualTo(5);
        assertThat(comment.getTitle()).isEqualTo("Amazing sound quality");
        assertThat(comment.getBody()).isEqualTo("The battery lasts forever");
        assertThat(comment.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void settersAndEqualityShouldWork() {
        Comment comment = new Comment();
        comment.setCommentId("comment-2");
        comment.setUserId("user-2");
        comment.setUsername("Jane");
        comment.setRating(4);
        comment.setTitle("Nice");
        comment.setBody("Great value");
        comment.setCreatedAt(Instant.parse("2026-06-16T10:00:00Z"));

        Comment sameComment = Comment.builder()
                .commentId("comment-2")
                .userId("user-2")
                .username("Jane")
                .rating(4)
                .title("Nice")
                .body("Great value")
                .createdAt(Instant.parse("2026-06-16T10:00:00Z"))
                .build();

        assertThat(comment).isEqualTo(sameComment);
        assertThat(comment.hashCode()).isEqualTo(sameComment.hashCode());
        assertThat(comment.toString()).contains("Jane");
    }
}
