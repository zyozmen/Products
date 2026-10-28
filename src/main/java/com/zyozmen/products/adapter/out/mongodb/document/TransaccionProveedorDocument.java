package com.zyozmen.products.adapter.out.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "Provider_Transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionProveedorDocument {
    @Id
    private String id;
    @Indexed
    @Field(targetType = FieldType.OBJECT_ID)
    private String proveedorId;
    @Field(targetType = FieldType.OBJECT_ID)
    private String loteId;
    private String tipo;
    private BigDecimal monto;
    private Instant fecha;
    private String observaciones;
}
