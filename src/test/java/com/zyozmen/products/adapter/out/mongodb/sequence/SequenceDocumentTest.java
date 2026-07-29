package com.zyozmen.products.adapter.out.mongodb.sequence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SequenceDocumentTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        SequenceDocument document = new SequenceDocument("productos_sequence", 7L);

        assertThat(document.getId()).isEqualTo("productos_sequence");
        assertThat(document.getSeq()).isEqualTo(7L);
    }
}
