package com.zyozmen.products.adapter.out.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "proveedores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorDocument {
    @Id
    private String id;
    private String nombre;
    @Indexed(unique = true)
    private String nit;
    private ContactoDocument contacto;
    private BigDecimal totalDeuda;
    private BigDecimal totalPagado;
    private BigDecimal saldoPendiente;
    private String estado;
    private Instant fechaCreacion;
    private Instant ultimaActualizacion;
    @Version
    private Long version;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactoDocument {
        private String telefono;
        private String email;
    }
}
