package com.zyozmen.products.adapter.out.mongodb.document;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CommentDocumentTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        Instant createdAt = Instant.parse("2026-06-15T14:30:00Z");

        CommentDocument document = CommentDocument.builder()
                .commentId("comment-1")
                .userId("user-1")
                .username("JohnDoe92")
                .rating(5)
                .title("Amazing sound quality")
                .body("The battery lasts forever")
                .createdAt(createdAt)
                .build();

        assertThat(document.getCommentId()).isEqualTo("comment-1");
        assertThat(document.getUserId()).isEqualTo("user-1");
        assertThat(document.getUsername()).isEqualTo("JohnDoe92");
        assertThat(document.getRating()).isEqualTo(5);
        assertThat(document.getTitle()).isEqualTo("Amazing sound quality");
        assertThat(document.getBody()).isEqualTo("The battery lasts forever");
        assertThat(document.getCreatedAt()).isEqualTo(createdAt);
    }
}
