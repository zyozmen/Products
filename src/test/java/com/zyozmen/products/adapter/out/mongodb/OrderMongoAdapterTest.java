package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.OrderDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.OrderMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.OrderMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderMongoAdapterTest {

    private final OrderMongoRepository repository = mock(OrderMongoRepository.class);
    private final OrderMongoMapper mapper = mock(OrderMongoMapper.class);
    private final SequenceGeneratorService sequence = mock(SequenceGeneratorService.class);
    private final MongoTemplate template = mock(MongoTemplate.class);
    private final OrderMongoAdapter adapter = new OrderMongoAdapter(repository, mapper, sequence, template);

    @Test
    void assignsSequenceWhenSavingOrderWithoutId() {
        Order order = Order.builder().username("cliente").status(OrderStatus.CREADA).build();
        OrderDocument document = OrderDocument.builder().id("7").status("CREADA").build();
        OrderDocument saved = OrderDocument.builder().id("7").status("CREADA").build();
        Order mapped = Order.builder().id(7L).status(OrderStatus.CREADA).build();
        when(sequence.nextSequence(SequenceGeneratorService.ORDER_SEQUENCE)).thenReturn(7L);
        when(mapper.toDocument(order)).thenReturn(document);
        when(repository.save(document)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(mapped);

        assertThat(adapter.save(order)).isSameAs(mapped);
        assertThat(order.getId()).isEqualTo(7L);
        verify(sequence).nextSequence(SequenceGeneratorService.ORDER_SEQUENCE);
    }

    @Test
    void retainsExistingIdAndMapsQueries() {
        Order order = Order.builder().id(9L).status(OrderStatus.CREADA).build();
        OrderDocument document = OrderDocument.builder().id("9").status("CREADA").build();
        when(mapper.toDocument(order)).thenReturn(document);
        when(repository.save(document)).thenReturn(document);
        when(mapper.toDomain(document)).thenReturn(order);
        when(repository.findById("9")).thenReturn(Optional.of(document));
        when(repository.findByUsernameOrderByCreatedAtDesc("cliente")).thenReturn(List.of(document));
        when(template.count(any(), eq(OrderDocument.class))).thenReturn(1L);
        when(template.find(any(), eq(OrderDocument.class))).thenReturn(List.of(document));

        assertThat(adapter.save(order)).isSameAs(order);
        assertThat(adapter.findById(9L)).contains(order);
        assertThat(adapter.findByUsername("cliente")).containsExactly(order);
        assertThat(adapter.findAll(null, null, null, null, null).getContent()).containsExactly(order);
        verify(sequence, never()).nextSequence(any());
    }

    @Test
    void appliesOptionalFiltersAndPageableToOrderSearch() {
        OrderDocument document = OrderDocument.builder().id("9").status("PAGADA").build();
        Order order = Order.builder().id(9L).status(OrderStatus.PAGADA).build();
        when(template.count(any(), eq(OrderDocument.class))).thenReturn(4L);
        when(template.find(any(), eq(OrderDocument.class))).thenReturn(List.of(document));
        when(mapper.toDomain(document)).thenReturn(order);

        var page = adapter.findAll(OrderStatus.PAGADA,
                java.time.Instant.parse("2026-11-01T00:00:00Z"),
                java.time.Instant.parse("2026-11-02T00:00:00Z"),
                " cliente ", PageRequest.of(1, 2));

        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent()).containsExactly(order);
        assertThat(page.getPageable()).isEqualTo(PageRequest.of(1, 2));

        when(template.find(any(), eq(OrderDocument.class))).thenReturn(List.of(document));
        assertThat(adapter.findAll(null, java.time.Instant.parse("2026-11-01T00:00:00Z"),
                null, " ", null).getContent()).containsExactly(order);
    }
}
