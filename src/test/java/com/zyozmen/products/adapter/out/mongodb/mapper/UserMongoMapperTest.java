package com.zyozmen.products.adapter.out.mongodb.mapper;

import com.zyozmen.products.adapter.out.mongodb.document.UserDocument;
import com.zyozmen.products.domain.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMongoMapperTest {

    private final UserMongoMapper mapper = new UserMongoMapper();

    @Test
    void mapsReferralCodeInBothDirections() {
        UserDocument document = UserDocument.builder()
                .username("juan123")
                .codigoReferido("ABCD2345")
                .build();

        User user = mapper.toDomain(document);
        UserDocument mappedDocument = mapper.toDocument(user);

        assertThat(user.getCodigoReferido()).isEqualTo("ABCD2345");
        assertThat(mappedDocument.getCodigoReferido()).isEqualTo("ABCD2345");
    }
}
