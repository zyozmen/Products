package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.domain.port.out.CodigoInventarioGeneratorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CodigoInventarioMongoAdapter implements CodigoInventarioGeneratorPort {

    private final SequenceGeneratorService sequenceGenerator;

    @Override
    public long siguienteNumeroLote() {
        return sequenceGenerator.nextSequence(SequenceGeneratorService.LOTE_SEQUENCE);
    }

    @Override
    public long siguienteNumeroContenedor() {
        return sequenceGenerator.nextSequence(SequenceGeneratorService.CONTENEDOR_SEQUENCE);
    }
}
