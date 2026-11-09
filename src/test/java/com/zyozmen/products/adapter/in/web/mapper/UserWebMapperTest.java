package com.zyozmen.products.adapter.in.web.mapper;

import com.zyozmen.products.adapter.in.web.dto.UserResponseDTO;
import com.zyozmen.products.domain.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserWebMapperTest {

    private final UserWebMapper mapper = new UserWebMapper();

    @Test
    void includesReferralCodeInUserResponse() {
        User user = User.builder()
                .username("juan123")
                .codigoReferido("ABCD2345")
                .build();

        UserResponseDTO response = mapper.toResponseDTO(user);

        assertThat(response.getCodigoReferido()).isEqualTo("ABCD2345");
    }
}
