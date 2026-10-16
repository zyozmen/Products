package com.zyozmen.products.adapter.out.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/**
 * Token revocado. Un índice TTL sobre expires_at lo elimina cuando el JWT
 * ya habría expirado por sí mismo.
 */
@Document(collection = "revoked_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevokedTokenMongoDocument {

    @Id
    private String tokenId;

    @Field("expires_at")
    private Date expiresAt;
}
