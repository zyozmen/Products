package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.LoteDocument;
import com.zyozmen.products.adapter.out.mongodb.document.TransaccionProveedorDocument;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorIndexInitializerTest {
    @Mock
    private MongoTemplate template;
    @Mock
    private IndexOperations lotes;
    @Mock
    private IndexOperations movimientos;

    @Test
    void garantizaCodigoUnicoYOrdenDeConsultasFifoEHistorial() {
        when(template.indexOps(LoteDocument.class)).thenReturn(lotes);
        when(template.indexOps(TransaccionProveedorDocument.class)).thenReturn(movimientos);

        new ProveedorIndexInitializer(template).run(null);

        var captor = ArgumentCaptor.forClass(IndexDefinition.class);
        verify(lotes, times(2)).ensureIndex(captor.capture());
        assertThat(captor.getAllValues().getFirst().getIndexKeys())
                .isEqualTo(new Document("codigoLote", 1));
        assertThat(captor.getAllValues().getFirst().getIndexOptions().getBoolean("unique")).isTrue();
        assertThat(captor.getAllValues().getLast().getIndexKeys())
                .isEqualTo(new Document("proveedorId", 1).append("fechaRecepcion", 1).append("_id", 1));
        verify(movimientos).ensureIndex(captor.capture());
        assertThat(captor.getValue().getIndexKeys())
                .isEqualTo(new Document("proveedorId", 1).append("fecha", -1).append("_id", -1));
    }
}
