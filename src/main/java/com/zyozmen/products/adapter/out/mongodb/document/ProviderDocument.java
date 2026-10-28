package com.zyozmen.products.adapter.out.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "Providers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderDocument {
    @Id
    private String id;
    private String nombre;
    @Indexed(unique = true)
    private String nit;
    private String telefono;
    private String correo;
    private FinancieroDocument financiero;
    private String estado;
    @Field("createdAt")
    private Instant fechaCreacion;
    @Field("updatedAt")
    private Instant ultimaActualizacion;
    @Version
    private Long version;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancieroDocument {
        private BigDecimal totalDeuda;
        private BigDecimal totalPagado;
        private BigDecimal saldoPendiente;
    }
}
