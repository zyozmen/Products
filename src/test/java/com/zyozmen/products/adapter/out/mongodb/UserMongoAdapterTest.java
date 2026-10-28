package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.UserDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.UserMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.UserMongoRepository;
import com.zyozmen.products.domain.model.Role;
import com.zyozmen.products.domain.model.IdentificationType;
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
    void existsByEmailShouldDelegateToRepository() {
        when(mongoRepository.existsByEmail("juan@example.com")).thenReturn(true);

        assertThat(adapter.existsByEmail("juan@example.com")).isTrue();
    }

    @Test
    void findByEmailShouldMapDocumentToDomain() {
        UserDocument document = UserDocument.builder().id("1").email("juan@example.com").build();
        User domainUser = User.builder().id("1").email("juan@example.com").build();

        when(mongoRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(document));
        when(mapper.toDomain(document)).thenReturn(domainUser);

        assertThat(adapter.findByEmail("juan@example.com")).contains(domainUser);
    }

    @Test
    void existsByTipoIdentificacionAndNumeroIdentificacionShouldReturnFalseWhenArgsNull() {
        assertThat(adapter.existsByTipoIdentificacionAndNumeroIdentificacion(null, "123")).isFalse();
        assertThat(adapter.existsByTipoIdentificacionAndNumeroIdentificacion(IdentificationType.CC, null)).isFalse();
    }

    @Test
    void existsByTipoIdentificacionAndNumeroIdentificacionShouldDelegateToRepository() {
        when(mongoRepository.existsByTipoIdentificacionAndNumeroIdentificacion("CC", "1018234567"))
                .thenReturn(true);

        boolean result = adapter.existsByTipoIdentificacionAndNumeroIdentificacion(
                IdentificationType.CC, "1018234567");

        assertThat(result).isTrue();
    }

    @Test
    void findByUsernameShouldMapDocumentToDomain() {
        UserDocument document = UserDocument.builder().id("1").username("juan123").build();
        User domainUser = User.builder().id("1").username("juan123").build();

        when(mongoRepository.findByUsername("juan123")).thenReturn(Optional.of(document));
        when(mapper.toDomain(document)).thenReturn(domainUser);

        Optional<User> result = adapter.findByUsername("juan123");

        assertThat(result).contains(domainUser);
    }

    @Test
    void saveShouldPreserveExistingIdWhenUserAlreadyExists() {
        User user = User.builder().username("juan123").role(Role.CLIENTE).build();
        UserDocument existingDocument = UserDocument.builder().id("existing-id").username("juan123").build();
        UserDocument documentToSave = UserDocument.builder().id("existing-id").username("juan123").build();
        UserDocument savedDocument = UserDocument.builder().id("existing-id").username("juan123").build();

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
        UserDocument documentToSave = UserDocument.builder().username("new-user").build();
        UserDocument savedDocument = UserDocument.builder().id("generated-id").username("new-user").build();

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
        UserDocument document = UserDocument.builder().id("1").username("juan123").build();
        User domainUser = User.builder().id("1").username("juan123").build();

        when(mongoRepository.findAll()).thenReturn(List.of(document));
        when(mapper.toDomain(document)).thenReturn(domainUser);

        List<User> result = adapter.findAll();

        assertThat(result).containsExactly(domainUser);
    }
}
