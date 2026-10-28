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
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "Lotes")
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
    @Field("costoTotalCalculado")
    private BigDecimal costoTotalLote;
    private BigDecimal montoPagadoLote;
    private BigDecimal saldoPendienteLote;
    private String estadoPago;
    private String estado;
    private List<ContenedorDocument> contenedores;
    private List<PresentacionDocument> presentaciones;
    @Field("fechaRecepcion")
    private Instant fechaIngreso;
    private BigDecimal abonoInicial;
    private BigDecimal costoTotalEmpaquetado;
    private Instant createdAt;
    private Instant updatedAt;
    @Version
    private Long version;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContenedorDocument {
        @Field(value = "contenedorId", targetType = FieldType.OBJECT_ID)
        private String id;
        private String codigoContenedor;
        @Field("unidadMedida")
        private String unidadBase;
        private String presentacionId;
        @Field("capacidadTotal")
        private BigDecimal capacidadUnidadesBase;
        private BigDecimal cantidadDisponible;
        private CepaDocument cepa;
        private BigDecimal subtotal;
        private String estado;
        private List<PresentacionInventarioDocument> presentaciones;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CepaDocument {
        private String id;
        private String nombre;
        private BigDecimal costoBaseUnidad;
        private BigDecimal precioBaseUnidad;
    }

    public record PresentacionInventarioDocument(
            @Field(targetType = FieldType.OBJECT_ID) String presentacionId,
            String sku, String nombre, BigDecimal contenidoUnidad, int unidadesCreadas,
            int stockDisponible, BigDecimal costoPresentacionCalculado, BigDecimal precioVenta,
            boolean activoMarketplace, boolean confirmacionMargenNegativo, ImageDocument fotografia) {
    }

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
