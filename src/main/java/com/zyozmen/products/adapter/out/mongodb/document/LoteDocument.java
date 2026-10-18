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
import java.util.List;

@Document(collection = "lotes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteDocument {
    @Id
    private String id;
    @Indexed
    @Field(targetType = FieldType.OBJECT_ID)
    private String proveedorId;
    @Indexed(unique = true)
    private String codigoLote;
    private BigDecimal costoTotalLote;
    private BigDecimal montoPagadoLote;
    private BigDecimal saldoPendienteLote;
    private String estadoPago;
    private List<PresentacionDocument> presentaciones;
    private Instant fechaIngreso;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresentacionDocument {
        private String presentacionId;
        private int cantidadUnidades;
        private BigDecimal costoUnitario;
        private BigDecimal subtotal;
    }
}
