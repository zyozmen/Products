package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.RevokedTokenMongoDocument;
import com.zyozmen.products.adapter.out.mongodb.repository.RevokedTokenMongoRepository;
import com.zyozmen.products.domain.port.out.RevokedTokenRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

/**
 * Adaptador de salida MongoDB para la lista de tokens revocados.
 */
@Service
@RequiredArgsConstructor
public class RevokedTokenMongoAdapter implements RevokedTokenRepositoryPort {

    private final RevokedTokenMongoRepository mongoRepository;

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        mongoRepository.save(new RevokedTokenMongoDocument(tokenId, Date.from(expiresAt)));
    }

    @Override
    public boolean isRevoked(String tokenId) {
        return mongoRepository.existsById(tokenId);
    }
}
