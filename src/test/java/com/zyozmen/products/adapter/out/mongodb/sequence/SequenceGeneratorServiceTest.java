package com.zyozmen.products.adapter.out.mongodb.sequence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SequenceGeneratorServiceTest {

    @Mock
    private MongoOperations mongoOperations;

    @InjectMocks
    private SequenceGeneratorService service;

    @Test
    void shouldReturnNextSequenceWhenCounterExists() {
        SequenceDocument counter = new SequenceDocument("productos_sequence", 7L);

        when(mongoOperations.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(SequenceDocument.class)))
                .thenReturn(counter);

        long result = service.nextSequence(SequenceGeneratorService.PRODUCTO_SEQUENCE);

        assertThat(result).isEqualTo(7L);
    }

    @Test
    void shouldReturnOneWhenCounterIsNull() {
        when(mongoOperations.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(SequenceDocument.class)))
                .thenReturn(null);

        long result = service.nextSequence(SequenceGeneratorService.PRODUCTO_SEQUENCE);

        assertThat(result).isEqualTo(1L);
    }
}
