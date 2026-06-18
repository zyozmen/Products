package com.zyozmen.products.adapter.out.mongodb.document;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDocument {

    @Field("category_id")
    private String categoryId;

    @Field("name")
    private String name;

    @Field("slug")
    private String slug;
}