package com.zyozmen.products.adapter.out.mongodb.document;

import java.time.Instant;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDocument {

    @Field("comment_id")
    private String commentId;

    @Field("user_id")
    private String userId;

    @Field("username")
    private String username;

    @Field("rating")
    private Integer rating;

    @Field("title")
    private String title;

    @Field("body")
    private String body;

    @Field("created_at")
    private Instant createdAt;
}