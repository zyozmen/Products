package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.mapper.ProductoMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.ProductoMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.domain.exception.ServiceUnavailableException;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.out.ProductoRepositoryPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de salida (Outbound Adapter) MongoDB.
 *
 * Implementa el puerto de salida definido en el dominio usando
 * Spring Data MongoDB. El dominio nunca ve esta clase: solo conoce la
 * interfaz ProductoRepositoryPort.
 *
 * Anotado con @Primary para que Spring lo prefiera sobre el adaptador JPA
 * cuando ambos estén en el classpath. Cambia el perfil activo para alternar
 * entre adaptadores sin modificar el dominio ni la capa de aplicación.
 *
 * Resilience4j:
 * - @Retry          → reintenta hasta 3 veces en errores transitorios de BD.
 * - @CircuitBreaker → abre el circuito al 50% de fallos en ventana de 10 llamadas.
 */
@Slf4j
@Primary
@Repository
@RequiredArgsConstructor
public class ProductoMongoAdapter implements ProductoRepositoryPort {

    private static final String CB_NAME = "productoRepository";

    private final ProductoMongoRepository mongoRepository;
    private final ProductoMongoMapper mapper;
    private final SequenceGeneratorService sequenceGenerator;

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "findAllFallback")
    public List<Producto> findAll() {
        return mongoRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    public List<Producto> findAllFallback(Throwable t) {
        log.warn("[Circuit Breaker][MongoDB] findAll no disponible. Causa: {}", t.getMessage());
        throw new ServiceUnavailableException("El servicio de productos no está disponible. Intente más tarde.", t);
    }

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "findByIdFallback")
    public Optional<Producto> findById(Long id) {
        return mongoRepository.findBySequenceId(id).map(mapper::toDomain);
    }

    public Optional<Producto> findByIdFallback(Long id, Throwable t) {
        log.warn("[Circuit Breaker][MongoDB] findById({}) no disponible. Causa: {}", id, t.getMessage());
        throw new ServiceUnavailableException("El servicio de productos no está disponible. Intente más tarde.", t);
    }

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "saveFallback")
    public Producto save(Producto producto) {
        if (producto.getId() == null) {
            long nextId = sequenceGenerator.nextSequence(SequenceGeneratorService.PRODUCTO_SEQUENCE);
            producto.setId(nextId);
            return mapper.toDomain(mongoRepository.save(mapper.toDocument(producto)));
        }

        // Update: preserve existing MongoDB _id to avoid duplicate key error
        return mongoRepository.findBySequenceId(producto.getId())
                .map(existing -> mapper.toDomain(
                        mongoRepository.save(mapper.toDocument(producto, existing.getId()))))
                .orElseGet(() -> mapper.toDomain(mongoRepository.save(mapper.toDocument(producto))));
    }

    public Producto saveFallback(Producto producto, Throwable t) {
        log.warn("[Circuit Breaker][MongoDB] save no disponible. Causa: {}", t.getMessage());
        throw new ServiceUnavailableException("El servicio de productos no está disponible. Intente más tarde.", t);
    }

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "existsByIdFallback")
    public boolean existsById(Long id) {
        return mongoRepository.existsBySequenceId(id);
    }

    public boolean existsByIdFallback(Long id, Throwable t) {
        log.warn("[Circuit Breaker][MongoDB] existsById({}) no disponible. Causa: {}", id, t.getMessage());
        throw new ServiceUnavailableException("El servicio de productos no está disponible. Intente más tarde.", t);
    }

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "deleteByIdFallback")
    public void deleteById(Long id) {
        mongoRepository.deleteBySequenceId(id);
    }

    public void deleteByIdFallback(Long id, Throwable t) {
        log.warn("[Circuit Breaker][MongoDB] deleteById({}) no disponible. Causa: {}", id, t.getMessage());
        throw new ServiceUnavailableException("El servicio de productos no está disponible. Intente más tarde.", t);
    }
}
