package com.zyozmen.products.adapter.in.web.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RecentCommentDTOTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        Instant createdAt = Instant.parse("2026-06-15T14:30:00Z");

        RecentCommentDTO dto = RecentCommentDTO.builder()
                .commentId("comment-1")
                .userId("user-1")
                .username("JohnDoe92")
                .rating(5)
                .title("Amazing sound quality")
                .body("The battery lasts forever")
                .createdAt(createdAt)
                .build();

        assertThat(dto.getCommentId()).isEqualTo("comment-1");
        assertThat(dto.getUserId()).isEqualTo("user-1");
        assertThat(dto.getUsername()).isEqualTo("JohnDoe92");
        assertThat(dto.getRating()).isEqualTo(5);
        assertThat(dto.getTitle()).isEqualTo("Amazing sound quality");
        assertThat(dto.getBody()).isEqualTo("The battery lasts forever");
        assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
    }
}
