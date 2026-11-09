package com.zyozmen.products.adapter.out.mongodb.document;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "Referidos")
@Data
@Builder
public class ReferralDocument {
    @Id
    private String id;
    @Field("IdReferente")
    private String idReferente;
    @Field("idReferido")
    private String idReferido;
    @Field("created_at")
    private Instant createdAt;
}
