package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.OrderMongoDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.OrderMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.OrderMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderStatus;
import com.zyozmen.products.domain.port.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Primary
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class OrderMongoAdapter implements OrderRepositoryPort {
    private final OrderMongoRepository repository;
    private final OrderMongoMapper mapper;
    private final SequenceGeneratorService sequenceGenerator;
    private final MongoTemplate mongoTemplate;

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            order.setId(sequenceGenerator.nextSequence(SequenceGeneratorService.ORDER_SEQUENCE));
        }
        return mapper.toDomain(repository.save(mapper.toDocument(order)));
    }

    @Override
    public Optional<Order> findById(Long id) {
        return repository.findById(id.toString()).map(mapper::toDomain);
    }

    @Override
    public List<Order> findByUsername(String username) {
        return repository.findByUsernameOrderByCreatedAtDesc(username).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Page<Order> findAll(OrderStatus status, Instant from, Instant to, String username, Pageable pageable) {
        List<Criteria> criteria = new ArrayList<>();
        if (status != null) {
            criteria.add(Criteria.where("status").is(status.name()));
        }
        if (from != null || to != null) {
            Criteria dateCriteria = Criteria.where("created_at");
            if (from != null) {
                dateCriteria.gte(from);
            }
            if (to != null) {
                dateCriteria.lt(to);
            }
            criteria.add(dateCriteria);
        }
        if (username != null && !username.isBlank()) {
            criteria.add(Criteria.where("username").is(username));
        }

        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }
        long count = mongoTemplate.count(query, OrderMongoDocument.class);
        query.with(Sort.by(Sort.Direction.DESC, "created_at"));
        if (pageable != null) {
            query.with(pageable);
        }
        List<Order> orders = mongoTemplate.find(query, OrderMongoDocument.class).stream().map(mapper::toDomain).toList();
        Pageable page = pageable == null ? Pageable.unpaged() : pageable;
        return new PageImpl<>(orders, page, count);
    }
}
