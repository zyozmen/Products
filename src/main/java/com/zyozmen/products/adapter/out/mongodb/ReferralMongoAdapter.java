package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ReferralDocument;
import com.zyozmen.products.adapter.out.mongodb.document.UserDocument;
import com.zyozmen.products.domain.model.Referral;
import com.zyozmen.products.domain.model.ReferralPage;
import com.zyozmen.products.domain.port.out.ReferralRepositoryPort;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReferralMongoAdapter implements ReferralRepositoryPort {
    private final MongoTemplate mongoTemplate;

    @Override
    public void insert(Referral referral) {
        if (referral.idReferente() == null || referral.idReferido() == null
                || referral.idReferente().equals(referral.idReferido()) || referral.createdAt() == null) {
            throw new IllegalArgumentException("El vinculo de referido requiere usuarios distintos y fecha");
        }
        // insert (not save) and the unique idReferido index prevent replacing a referrer.
        mongoTemplate.insert(ReferralDocument.builder()
                .idReferente(referral.idReferente()).idReferido(referral.idReferido())
                .createdAt(referral.createdAt()).build());
    }

    @Override
    public ReferralPage findByReferenteId(String idReferente, int page, int size) {
        Query filter = Query.query(Criteria.where("idReferente").is(idReferente));
        long total = mongoTemplate.count(filter, ReferralDocument.class);
        Query paginated = Query.of(filter).with(Sort.by(Sort.Direction.DESC, "createdAt", "id"))
                .skip((long) page * size).limit(size);
        var links = mongoTemplate.find(paginated, ReferralDocument.class);
        if (links.isEmpty()) {
            return new ReferralPage(java.util.List.of(), page, size, total);
        }
        Query usersQuery = Query.query(Criteria.where("id")
                .in(links.stream().map(ReferralDocument::getIdReferido).toList()));
        usersQuery.fields().include("id", "username", "nombre", "apellido");
        Map<String, UserDocument> users = mongoTemplate.find(usersQuery, UserDocument.class).stream()
                .collect(Collectors.toMap(UserDocument::getId, Function.identity()));
        var content = links.stream().map(link -> {
            UserDocument user = users.get(link.getIdReferido());
            if (user == null) {
                throw new IllegalStateException("El usuario referido no existe: " + link.getIdReferido());
            }
            return new ReferralPage.Item(user.getUsername(), user.getNombre(), user.getApellido(),
                    link.getCreatedAt());
        }).toList();
        return new ReferralPage(content, page, size, total);
    }
}
