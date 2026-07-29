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
}
