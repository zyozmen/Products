package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ReferralDocument;
import com.zyozmen.products.adapter.out.mongodb.document.UserDocument;
import com.zyozmen.products.domain.model.Referral;
import java.time.Instant;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReferralMongoAdapterTest {
    private final MongoTemplate template = mock(MongoTemplate.class);
    private final ReferralMongoAdapter adapter = new ReferralMongoAdapter(template);

    @Test
    void insertsRelationshipAndDoesNotUpsertOrReplaceIt() {
        Instant now = Instant.now();
        adapter.insert(new Referral("owner", "new", now));
        var doc = ArgumentCaptor.forClass(ReferralDocument.class);
        verify(template).insert(doc.capture());
        assertThat(doc.getValue().getIdReferente()).isEqualTo("owner");
        assertThat(doc.getValue().getIdReferido()).isEqualTo("new");
        assertThat(doc.getValue().getCreatedAt()).isEqualTo(now);
        verifyNoMoreInteractions(template);
    }

    @Test
    void duplicateAssociationFailureIsNotSwallowed() {
        when(template.insert(any(ReferralDocument.class))).thenThrow(new DuplicateKeyException("already linked"));
        assertThatThrownBy(() -> adapter.insert(new Referral("owner", "new", Instant.now())))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void invalidRelationshipIsRejected() {
        assertThatThrownBy(() -> adapter.insert(new Referral("same", "same", Instant.now())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.insert(new Referral("owner", null, Instant.now())))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(template);
    }

    @Test
    void queriesPaginatedLinksAndLoadsOnlySafeUserFieldsInOneBatch() {
        var instant = Instant.parse("2026-11-09T15:00:00Z");
        var link = ReferralDocument.builder().idReferente("owner").idReferido("new")
                .createdAt(instant).build();
        when(template.count(any(Query.class), eq(ReferralDocument.class))).thenReturn(21L);
        when(template.find(any(Query.class), eq(ReferralDocument.class))).thenReturn(List.of(link));
        when(template.find(any(Query.class), eq(UserDocument.class))).thenReturn(List.of(
                UserDocument.builder().id("new").username("invited").nombre("Nombre").apellido("Apellido").build()));

        var result = adapter.findByReferenteId("owner", 1, 20);

        var linksQuery = ArgumentCaptor.forClass(Query.class);
        verify(template).find(linksQuery.capture(), eq(ReferralDocument.class));
        assertThat(linksQuery.getValue().getQueryObject()).isEqualTo(new Document("idReferente", "owner"));
        assertThat(linksQuery.getValue().getSkip()).isEqualTo(20);
        assertThat(linksQuery.getValue().getLimit()).isEqualTo(20);
        assertThat(linksQuery.getValue().getSortObject())
                .isEqualTo(new Document("createdAt", -1).append("id", -1));
        var usersQuery = ArgumentCaptor.forClass(Query.class);
        verify(template).find(usersQuery.capture(), eq(UserDocument.class));
        assertThat(usersQuery.getValue().getFieldsObject())
                .isEqualTo(new Document("id", 1).append("username", 1).append("nombre", 1).append("apellido", 1));
        assertThat(result.totalElements()).isEqualTo(21);
        assertThat(result.content().getFirst().username()).isEqualTo("invited");
        assertThat(result.content().getFirst().createdAt()).isEqualTo(instant);
    }

    @Test
    void emptyPageDoesNotQueryUsers() {
        when(template.find(any(Query.class), eq(ReferralDocument.class))).thenReturn(List.of());
        assertThat(adapter.findByReferenteId("owner", 0, 20).content()).isEmpty();
        verify(template, never()).find(any(Query.class), eq(UserDocument.class));
    }
}
