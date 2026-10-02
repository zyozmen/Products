package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.UserMongoDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.UserMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.UserMongoRepository;
import com.zyozmen.products.domain.model.Role;
import com.zyozmen.products.domain.model.TipoIdentificacion;
import com.zyozmen.products.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMongoAdapterTest {

    @Mock
    private UserMongoRepository mongoRepository;

    @Mock
    private UserMongoMapper mapper;

    @InjectMocks
    private UserMongoAdapter adapter;

    @Test
    void existsByUsernameShouldDelegateToRepository() {
        when(mongoRepository.existsByUsername("juan123")).thenReturn(true);

        assertThat(adapter.existsByUsername("juan123")).isTrue();
    }

    @Test
    void existsByTipoIdentificacionAndNumeroIdentificacionShouldReturnFalseWhenArgsNull() {
        assertThat(adapter.existsByTipoIdentificacionAndNumeroIdentificacion(null, "123")).isFalse();
        assertThat(adapter.existsByTipoIdentificacionAndNumeroIdentificacion(TipoIdentificacion.CC, null)).isFalse();
    }

    @Test
    void existsByTipoIdentificacionAndNumeroIdentificacionShouldDelegateToRepository() {
        when(mongoRepository.existsByTipoIdentificacionAndNumeroIdentificacion("CC", "1018234567"))
                .thenReturn(true);

        boolean result = adapter.existsByTipoIdentificacionAndNumeroIdentificacion(
                TipoIdentificacion.CC, "1018234567");

        assertThat(result).isTrue();
    }

    @Test
    void findByUsernameShouldMapDocumentToDomain() {
        UserMongoDocument document = UserMongoDocument.builder().id("1").username("juan123").build();
        User domainUser = User.builder().id("1").username("juan123").build();

        when(mongoRepository.findByUsername("juan123")).thenReturn(Optional.of(document));
        when(mapper.toDomain(document)).thenReturn(domainUser);

        Optional<User> result = adapter.findByUsername("juan123");

        assertThat(result).contains(domainUser);
    }

    @Test
    void saveShouldPreserveExistingIdWhenUserAlreadyExists() {
        User user = User.builder().username("juan123").role(Role.CLIENTE).build();
        UserMongoDocument existingDocument = UserMongoDocument.builder().id("existing-id").username("juan123").build();
        UserMongoDocument documentToSave = UserMongoDocument.builder().id("existing-id").username("juan123").build();
        UserMongoDocument savedDocument = UserMongoDocument.builder().id("existing-id").username("juan123").build();

        when(mongoRepository.findByUsername("juan123")).thenReturn(Optional.of(existingDocument));
        when(mapper.toDocument(user, "existing-id")).thenReturn(documentToSave);
        when(mongoRepository.save(documentToSave)).thenReturn(savedDocument);
        when(mapper.toDomain(savedDocument)).thenReturn(user);

        User result = adapter.save(user);

        assertThat(result).isEqualTo(user);
        verify(mapper).toDocument(user, "existing-id");
    }

    @Test
    void saveShouldUseNullIdWhenUserIsNew() {
        User user = User.builder().username("new-user").build();
        UserMongoDocument documentToSave = UserMongoDocument.builder().username("new-user").build();
        UserMongoDocument savedDocument = UserMongoDocument.builder().id("generated-id").username("new-user").build();

        when(mongoRepository.findByUsername("new-user")).thenReturn(Optional.empty());
        when(mapper.toDocument(user, null)).thenReturn(documentToSave);
        when(mongoRepository.save(documentToSave)).thenReturn(savedDocument);
        when(mapper.toDomain(savedDocument)).thenReturn(user);

        User result = adapter.save(user);

        assertThat(result).isEqualTo(user);
        verify(mapper).toDocument(user, null);
    }

    @Test
    void findAllShouldMapEveryDocument() {
        UserMongoDocument document = UserMongoDocument.builder().id("1").username("juan123").build();
        User domainUser = User.builder().id("1").username("juan123").build();

        when(mongoRepository.findAll()).thenReturn(List.of(document));
        when(mapper.toDomain(document)).thenReturn(domainUser);

        List<User> result = adapter.findAll();

        assertThat(result).containsExactly(domainUser);
    }
}
